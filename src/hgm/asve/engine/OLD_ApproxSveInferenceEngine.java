package hgm.asve.engine;

import hgm.IQuery;
import hgm.Variable;
import hgm.asve.factor.OLD_IFactor;
import hgm.asve.factory.OLD_FactorFactory;
import hgm.asve.model.BayesianGraphicalModel;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 19/09/13
 * Time: 4:02 PM
 */
@Deprecated
public class OLD_ApproxSveInferenceEngine<F extends OLD_IFactor> implements InferenceEngine<F> {
    private int _numberOfFactorsLeadingToJointFactorApproximation;

    private BayesianGraphicalModel<F> _model;
    private OLD_FactorFactory<F> _factory;

    class FactorSet extends HashSet<F> {
        public FactorSet() {
            super();
        }

        public FactorSet(Collection<? extends F> c) {
            super(c);
        }

        public Set<Variable> getScopeVars() {
            Set<Variable> vars = new HashSet<Variable>();
            for (F f : this) {
                vars.addAll(f.getScopeVars());
            }
            return vars;
        }
    }

    public OLD_ApproxSveInferenceEngine(BayesianGraphicalModel model, OLD_FactorFactory<F> factory, int numberOfFactorsLeadingToJointFactorApproximation) {
        _model = model;
        _factory = factory;
        _numberOfFactorsLeadingToJointFactorApproximation = numberOfFactorsLeadingToJointFactorApproximation;
    }

    @Override
    public F infer(IQuery query) {
        //topologically score the graph:
        Map<F, Integer> factorScoreMap = new HashMap<F, Integer>();
        List<F> qFactors = _model.getAssociatedFactors(query.getQueries());
        List<Variable> nonRemovableVariables = new ArrayList<Variable>();
        nonRemovableVariables.addAll(query.getQueries());
        nonRemovableVariables.addAll(query.getNonInstantiatedEvidenceVariables());

        for (F q : qFactors) {
            topologicallyScore(q, factorScoreMap);
        }

        FactorSet unprocessedFactors = new FactorSet(factorScoreMap.keySet()); //I did not use the keySet directly fearing maybe in future I'll need it!

        Map<Integer, List<F>> scoreFactorsMap = getScoreFactorsMap(factorScoreMap);

        //process this data structure:
        List<Integer> scores = new ArrayList<Integer>(scoreFactorsMap.keySet());
        Collections.sort(scores);
        // In our terminology, a "(factor) joint set" is a set of factors that if multiplied together form a joint distribution.
        Set<FactorSet> collectionOfJointFactorSets = new HashSet<FactorSet>();
        for (Integer score : scores) {
            List<F> sameScoreFactors = scoreFactorsMap.get(score);
            while (sameScoreFactors.size() > 0) {
                //1. Find a new candidate factor to be a seed of a new set of joints:
                F chosenF = heuristicallyChooseBestFactor(sameScoreFactors);
                sameScoreFactors.remove(chosenF);
                unprocessedFactors.remove(chosenF);
                FactorSet newJointFactorSet = new FactorSet();
                newJointFactorSet.add(chosenF);

                //2. Transfer to it all members of any joint-set that contains any of its parent variables:
                // E.g. if parents(X)={A,B} (i.e. f(X,A,B) where f is the factor associated with variable X) then:
                // adding f(X,A,B) to { {f1(A,C,D),f2(C,E)}, {f3(G,H),f4(I)} }and performing step 2 ends in:
                // { {f3(G,H),f4(I)}, {f(X,A,B),f1(A,C,D),f2(C,E)} }
                Set<Variable> chosenFactorVars = chosenF.getScopeVars();//model.get.getParents(chosenF);
                for (Iterator<FactorSet> jointSetIterator = collectionOfJointFactorSets.iterator();
                     jointSetIterator.hasNext(); ) {
                    FactorSet jointSet = jointSetIterator.next();
                    Set<Variable> jointSetScopeVars = jointSet.getScopeVars();
                    if (!Collections.disjoint(jointSetScopeVars, chosenFactorVars)) {
                        newJointFactorSet.addAll(jointSet);
                        //remove the previous set:
                        jointSetIterator.remove(); // safe way to "setOfJointFactorSets.remove(jointSet)" in iteration-loop
                    }
                }

                //3. In case there are variables exclusively used in the newly made "set of joint factors"
                // (i.e. not used in un processed factors),
                // multiply them and marginalize out the common variable (of course not if it is in query).
                // The whole concept of "set of joint factors" is to perform multiplication lazily hoping that
                // by variable elimination, some redundant multiplications can be prevented.
                Set<Variable> varScopeOfUnprocessedFactors = unprocessedFactors.getScopeVars();
                Set<Variable> removableVarsExclusivelyUsedInNewJointFactorSet = newJointFactorSet.getScopeVars();
                removableVarsExclusivelyUsedInNewJointFactorSet.removeAll(varScopeOfUnprocessedFactors);
                removableVarsExclusivelyUsedInNewJointFactorSet.removeAll(nonRemovableVariables);

                if (!removableVarsExclusivelyUsedInNewJointFactorSet.isEmpty()) {
                    F joint = _factory.multiply(newJointFactorSet);
                    for (Variable varToMarginalize : removableVarsExclusivelyUsedInNewJointFactorSet) {
                        joint = _factory.marginalize(joint, varToMarginalize);
                    }
                    newJointFactorSet.clear();
                    newJointFactorSet.add(joint);
                }


                //4. If necessary, simplify the new joint factor set:
                if (approximationIsNecessary(newJointFactorSet)) {
                    newJointFactorSet = new FactorSet(Arrays.asList(_factory.approximate(_factory.multiply(newJointFactorSet))));
                }

                //5. Add the new joint factor set to the relevant set:
                collectionOfJointFactorSets.add(newJointFactorSet);
            }
//            System.out.println("After score:= " + score + ", collection: " + collectionOfJointFactorSets);
        }

        // Make the final joint factor.
        // Nothing should be remained to marginalize out:
        FactorSet allRemainedFactors = new FactorSet();
        //each collection should only contain a single joint factor set and collections should be disjoint (in any sense).
        for (FactorSet jointFactorSet : collectionOfJointFactorSets) {
            // only one member should be remained in it:
            if (jointFactorSet.size() != 1) throw new RuntimeException("only one member should be remained in it");
            allRemainedFactors.addAll(jointFactorSet);
        }
        return _factory.multiply(allRemainedFactors);
    }

    private boolean approximationIsNecessary(FactorSet factorSet) {
        //TODO what is the good heuristic for approximation?
        return factorSet.getScopeVars().size() >= _numberOfFactorsLeadingToJointFactorApproximation;
    }

    private F heuristicallyChooseBestFactor(List<F> factors) {
        //todo definitely needs to be re-written. NOW DUMMY:
        if (factors.isEmpty()) throw new RuntimeException("what?");
        return factors.get(0);
    }


    private Map<Integer, List<F>> getScoreFactorsMap(Map<F, Integer> factorScoreMap) {
        Map<Integer, List<F>> m = new HashMap<Integer, List<F>>();
        for (F f : factorScoreMap.keySet()) {
            Integer s = factorScoreMap.get(f);
            List<F> fs = m.get(s);
            if (fs == null) {
                fs = new ArrayList<F>();
                m.put(s, fs);
            }
            if (fs.contains(f)) {
                throw new RuntimeException("I do not know what has happened!");
            }
            fs.add(f);
        }
        return m;
    }

    private int topologicallyScore(F f, Map<F, Integer> factorScoreMap) {
        Integer currentScore = factorScoreMap.get(f);
        if (currentScore == null) {
            currentScore = 0;
        }

        int maxParentScore = 0;
        Set<F> parents = _model.getParents(f);
        for (F parent : parents) {
            maxParentScore = Math.max(maxParentScore, topologicallyScore(parent, factorScoreMap));
        }
        currentScore = Math.max(currentScore, maxParentScore + 1);
        factorScoreMap.put(f, currentScore);
        return currentScore;
    }

    @Override
    public F infer(IQuery query, final List<Variable> varOrdering) {
        throw new RuntimeException("not implemented!");
//        List<F> factorOrdering = new ArrayList<F>(varOrdering.size());
//        for (int i = 0; i < varOrdering.size(); i++) {
//            factorOrdering.set(i, _model.getFactor(varOrdering.get(i)));
//        }
//        return inferr(query, factorOrdering);
    }
/*

    private F inferr(IQuery query, final List<F> factorOrdering) {
        List<Variable> qVars = query.getQueries();
        List<F> qFactors = _model.getFactors(qVars);

        //sort query variables due to the given ordering:
        sortFactors(qFactors, factorOrdering);

        System.out.println("qVars = " + qFactors + " sorted based on the order: " + factorOrdering);

        List<F> sortedNodes = new ArrayList<F>();

        for (F q : qFactors) {
            List<F> ancestors = getAncestors(q, factorOrdering, sortedNodes);
            System.out.println("ancestors = " + ancestors + " of " + q);
            if (shouldBeApproximated(q, ancestors)) {
                approximate(ancestors);
            }
        }

        System.out.println("sortedFactors = " + sortedNodes);


        return null;
    }
*/

/*
    private F approximate(List<F> ancestors) {
        //Since we deal with a bayesian network, the product of all ancestors end in a joint distribution
        return _factory.createJoint(ancestors);
    }

    private boolean shouldBeApproximated(F factor, List<F> ancestors) {
        // a simple heuristic that may be changed in future....
        return (ancestors.size() >= ApproxSveInferenceEngine._NUM_ANCESTORS_TO_APPROXIMATE);
    }
*/

    /**
     * It is guaranteed that the parents of each variable/factor are entered in the list prior to it.
     * <p/>
     * param q query variable
     * return self and ancestors
     */
/*
    private List<F> getAncestors(F q, List<F> preferableVarOrdering
            , List<F> alreadyObservedNodes) {
        List<F> parents = new ArrayList<F>(_model.getParents(q));
        sortFactors(parents, preferableVarOrdering);

        List<F> ancestors = new ArrayList<F>(); //ancestors includes self!
        if (!alreadyObservedNodes.contains(q)) {
            ancestors.add(q);
        }

        for (F p : parents) {
            if (!alreadyObservedNodes.contains(p)) { // if the parent is not processed yet. NOTE:factors should not be processed
                ancestors.addAll(getAncestors(p, preferableVarOrdering, alreadyObservedNodes));
            }
        }

        return ancestors;
    }
*/
    private void sortFactors(List<F> listToBeSorted, final List<F> preferableVarOrdering) {
        Collections.sort(listToBeSorted, new Comparator<F>() {
            @Override
            public int compare(F v1, F v2) {
                int i1 = preferableVarOrdering.indexOf(v1);
                int i2 = preferableVarOrdering.indexOf(v2);
                if (i1 < 0 || i2 < 0) throw new RuntimeException("unknown variable!");

                if (i1 < i2) return -1;
                if (i1 > i2) return 1;

                return 0;
            }
        });
    }

}
