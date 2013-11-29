package hgm.asve.cnsrv.infer;

import hgm.asve.cnsrv.factory.ModelBasedXaddFactorFactory;
import hgm.asve.cnsrv.gm.FBQuery;
import hgm.asve.cnsrv.FactorSet;
import hgm.asve.cnsrv.factor.Factor;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 19/09/13
 * Time: 4:02 PM
 */
public class ApproxSveInferenceEngine {
    public static boolean NORMALIZE_RESULT = true;//true;//false; //todo tempo

    private ModelBasedXaddFactorFactory factory;
    private Records _records;

    public ApproxSveInferenceEngine(ModelBasedXaddFactorFactory factory) {

        this.factory = factory;
        _records = new Records("approximate-SVE");

    }

    //NOTE: specific order can cause bug, so it is a hack for testing only:
    @Deprecated
    Factor inferHack(List<String> varOrder, List<String> nonRemovableVariables) {
        Map<String, Integer> varScoreMap = new HashMap<String, Integer>();
        for (int i = 0; i < varOrder.size(); i++) {
            varScoreMap.put(varOrder.get(i), i + 1);
        }
        return infer(varScoreMap, nonRemovableVariables);
    }


    /**
     * @return inference (of query factors) s.t.:
     *         1. The children are processed after parents.
     *         2. Only query/evidence factors and their ancestors are taken into account.
     *         3. Factor multiplication (and approximation) is performed lazily (i.e. as late as possible)
     */
    public Factor infer() {
        System.out.println("NORMALIZE_RESULT = " + NORMALIZE_RESULT);
        FBQuery query = factory.getQuery();
        _records.set("query.variables", query.getQueryVariables().toString());
        _records.set("query.continuous.instantiated.evidence", query.getContinuousInstantiatedEvidence().toString());
        _records.set("query.boolean.instantiated.evidence", query.getBooleanInstantiatedEvidence().toString());

        //topologically score the graph:
        List<String> nonRemovableVariables = new ArrayList<String>();
        nonRemovableVariables.addAll(query.getQueryVariables());
        nonRemovableVariables.addAll(query.getNonInstantiatedEvidenceVariables());
        nonRemovableVariables.addAll(query.fetchInstantiatedEvidenceVariables()); //although instantiated variables are omitted, there corresponding factor is not.
        _records.set("non.removable.variables", nonRemovableVariables.toString());

        Map<String, Integer> varScoreMap = new HashMap<String, Integer>();
        for (String q : nonRemovableVariables) {
            topologicallyScoreSelfAndAncestors(q, varScoreMap);
        }
        _records.set("all.variables.taken.into.account", varScoreMap.keySet().toString());

        return infer(varScoreMap, nonRemovableVariables);
    }

    private Factor infer(Map<String, Integer> varScoreMap, List<String> nonRemovableVariables) {

        //convert varScoreMap to factorScoreMap:
        Map<Factor, Integer> factorScoreMap = new HashMap<Factor, Integer>();
        for (String v : varScoreMap.keySet()) {
            factorScoreMap.put(factory.getAssociatedInstantiatedFactor(v), varScoreMap.get(v));
        }

        Set<Factor> unprocessedFactors = new HashSet<Factor>(factorScoreMap.keySet()); //I did not use the keySet directly fearing maybe in future I'll need it!

        Map<Integer, List<Factor>> scoreFactorsMap = getScoreFactorsMap(factorScoreMap);

        //process this data structure:
        List<Integer> scores = new ArrayList<Integer>(scoreFactorsMap.keySet());
        Collections.sort(scores);

        Set<Factor> processedJointFactors = new HashSet<Factor>();

        for (Integer score : scores) {
            List<Factor> sameScoreFactors = scoreFactorsMap.get(score);
            while (sameScoreFactors.size() > 0) {

                //1. Find a new candidate factor to be a seed of a new set of joints:
                Factor chosenF = heuristicallyChooseBestFactor(sameScoreFactors);
                _records.desiredVariableEliminationOrder.add(factory.getAssociatedVariable(chosenF));

                sameScoreFactors.remove(chosenF);
                unprocessedFactors.remove(chosenF);

                //2. Multiply to it all joint factors that contain any of its scope variables:
                Set<String> chosenFactorVars = chosenF.getScopeVars();
                for (Iterator<Factor> jointFactorsIterator = processedJointFactors.iterator(); jointFactorsIterator.hasNext(); ) {
                    Factor processedFactor = jointFactorsIterator.next();
                    Set<String> factorScopeVars = processedFactor.getScopeVars();
                    if (!Collections.disjoint(factorScopeVars, chosenFactorVars)) {
                        chosenF = factory.approximateMultiply(Arrays.asList(chosenF, processedFactor));
                        //remove the previous set:
                        jointFactorsIterator.remove(); // this is a safe way to "setOfJointFactorSets.remove(jointSet)" in iteration-loop
                    }
                }

                //3. In case there are variables exclusively used in the newly made "joint factor"
                // (i.e. not used in unprocessed factors),
                // marginalize out the common variable (of course not if it is in query).
                Set<String> varScopeOfUnprocessedFactors = new HashSet<String>();
                for (Factor unprocessedFactor : unprocessedFactors) {
                    varScopeOfUnprocessedFactors.addAll(unprocessedFactor.getScopeVars());
                }
                Set<String> removableVarsExclusivelyUsedInNewJointFactor = new HashSet<String>(chosenF.getScopeVars());
                removableVarsExclusivelyUsedInNewJointFactor.removeAll(varScopeOfUnprocessedFactors);
                removableVarsExclusivelyUsedInNewJointFactor.removeAll(nonRemovableVariables);

                if (!removableVarsExclusivelyUsedInNewJointFactor.isEmpty()) {
                    for (String varToMarginalize : removableVarsExclusivelyUsedInNewJointFactor) {
                        System.out.println("varToMarginalize = " + varToMarginalize);
//                        System.out.println("2.1 chosenF = " + chosenF);
                        chosenF = factory.marginalize(chosenF, varToMarginalize); //todo: do I need approximation here as well?
//                        System.out.println("2.2 chosenF = " + chosenF);
                        _records.variablesActuallyMarginalized.add(varToMarginalize);
                    }
                }

//                System.out.println("3. chosenF.getHelpingText() = " + chosenF.getHelpingText());

                //todo are these necessary?
//                String preApproxFactorRecord = _records.factorSetRecordStr(new FactorSet(Arrays.asList(chosenF)), false);//todo factor sect record str should be modified to factor record str
//                String postApproxFactorRecord = _records.factorSetRecordStr(newJointFactorSet, true);
//                _records.recordFactorSetApproximation(preApproxFactorRecord, postApproxFactorRecord); //todo what is this?

                //5. Add the new joint factor set to the relevant set:
                processedJointFactors.add(chosenF);

                Set<Factor> factorsInUse = new HashSet<Factor>(processedJointFactors);
                factorsInUse.addAll(factorScoreMap.keySet());
                factory.flushFactorsExcept(factorsInUse);
            } //end while
        }

        // Make the final joint factor.
        // Nothing should be remained to marginalize out:
//        FactorSet allRemainedFactors = new FactorSet();
        //The scope of each factor set of each collection should only contain query variables and collections should be disjoint (in any sense).
//        for (FactorSet jointFactorSet : collectionOfJointFactorSets) {
//            allRemainedFactors.addAll(jointFactorSet);
//        }
        Factor multipliedRemainedFactors = factory.approximateMultiply(processedJointFactors/*allRemainedFactors*/);

//        _factory.getVisualizer().visualizeFactor(multipliedRemainedFactors, ("Last step before normalization"));

        _records.recordFactor(multipliedRemainedFactors);


        Factor finalResult;
        if (NORMALIZE_RESULT) {
            finalResult = factory.normalize(multipliedRemainedFactors);
        } else {
            System.err.println("Warning: final normalization is not performed in Approx SVE");
            finalResult = multipliedRemainedFactors;
        }

//        _factory.getVisualizer().visualizeFactor(finalResult, ("normalized final:"));

        _records.recordFinalResult(finalResult);

        factory.makePermanent(Arrays.asList(finalResult));
        factory.flushFactorsExcept(Collections.EMPTY_LIST);
        return finalResult;
    }

    private Factor heuristicallyChooseBestFactor(List<Factor> factors) {
        //todo definitely needs to be re-written. NOW DUMMY:
        if (factors.isEmpty()) throw new RuntimeException("what?");
        return factors.get(0);
    }


    private Map<Integer, List<Factor>> getScoreFactorsMap(Map<Factor, Integer> factorScoreMap) {
        Map<Integer, List<Factor>> m = new HashMap<Integer, List<Factor>>();
        for (Factor f : factorScoreMap.keySet()) {
            Integer s = factorScoreMap.get(f);
            List<Factor> fs = m.get(s);
            if (fs == null) {
                fs = new ArrayList<Factor>();
                m.put(s, fs);
            }
            if (fs.contains(f)) {
                throw new RuntimeException("I do not know what has happened!");
            }
            fs.add(f);
        }
        return m;
    }

    private int topologicallyScoreSelfAndAncestors(String var, Map<String, Integer> variableScoreMap) {
        Integer currentScore = variableScoreMap.get(var);
        if (currentScore == null) {
            currentScore = 0;
        }

        int maxParentScore = 0;
        Set<String> parents = factory.getParents(var);
        if (parents == null) {
            throw new RuntimeException("NULL parents for " + var);
        }
        for (String parent : parents) {
            maxParentScore = Math.max(maxParentScore, topologicallyScoreSelfAndAncestors(parent, variableScoreMap));
        }
        currentScore = Math.max(currentScore, maxParentScore + 1);
        variableScoreMap.put(var, currentScore);
        return currentScore;
    }

    public Records getRecords() {
        return _records;
    }
}
