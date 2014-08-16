package hgm.poly.gm;

import hgm.asve.Pair;
import hgm.poly.ConstrainedExpression;
import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;
import hgm.poly.PolynomialFactory;
import hgm.poly.sampling.SamplerInterface;
import hgm.poly.sampling.frac.SymbolicFractionGibbsSampler;
import hgm.sampling.SamplingFailureException;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 23/07/14
 * Time: 5:05 PM
 */
public class SymbolicGraphicalModelHandler {
    public static final boolean DEBUG = false;

    PiecewiseExpression<Fraction> makeJoint(GraphicalModel gm, List<String> queryVars, Map<String, Double> evidence) {
        Pair<PiecewiseExpression<Fraction>, List<DeterministicFactor>> jointAndEliminatedStochasticVars = makeJointAndEliminatedStochasticVars(gm, queryVars, evidence);
        return jointAndEliminatedStochasticVars.getFirstEntry();
    }

    Pair<PiecewiseExpression<Fraction> /*joint*/, List<DeterministicFactor> /*eliminatedStochasticVars*/> makeJointAndEliminatedStochasticVars(GraphicalModel gm, List<String> queryVars, Map<String, Double> evidence) {
        //step 0.
        Set<String> queryAndEvidenceVars = new HashSet<String>(queryVars.size() + evidence.size());
        queryAndEvidenceVars.addAll(queryVars);
        queryAndEvidenceVars.addAll(evidence.keySet());

        List<Factor> originalFactors = gm.allInferenceRelevantFactors(queryAndEvidenceVars); //in BNs this is the factors of query, evidence and their ancestors.
        debug("\n * \t originalFactors = " + originalFactors);

        //step 1.
        List<Factor> factors = instantiateObservedFactors(originalFactors, evidence);

        debug("\n * \t factors instantiated= " + factors);
        //step 2.
        isolateDeterministicFactors(factors);
        debug("\n * \t factors det. isolated = " + factors);

        //step 3.
        List<DeterministicFactor> eliminatedStochasticVars = reduceDimension(factors, evidence);
        debug("\n * \t factors with reduced dimension = " + factors);
        debug("\n * \t \t eliminated stochastic vars" + eliminatedStochasticVars);

        //step 4.
        PiecewiseExpression<Fraction> finalJoint = makeStochasticFactorsJoint(factors);
        debug("\n * \t finalJoint = " + finalJoint);

        return new Pair<PiecewiseExpression<Fraction>, List<DeterministicFactor>>(finalJoint, eliminatedStochasticVars);
    }

    private PiecewiseExpression<Fraction> makeStochasticFactorsJoint(List<Factor> factors) {
        //nulls are eliminated.
        PolynomialFactory factory = factors.get(0).getFactory();

        PiecewiseExpression<Fraction> result = new PiecewiseExpression<Fraction>(new ConstrainedExpression<Fraction>(factory.makeFraction("1"), new HashSet<Fraction>())); //one
        for (Factor factor : factors) {
            if (factor != null) {
                result = result.multiply(((StochasticVAFactor) factor).getPiecewiseFraction());
            }
        }

        return result;
    }

    List<Factor> instantiateObservedFactors(List<Factor> factors, Map<String, Double> evidence) {
        List<Factor> results = new ArrayList<Factor>(factors.size());
        for (Factor factor : factors) {
            results.add(factor.substitution(evidence));
        }
        return results;
    }

    void isolateDeterministicFactors(List<Factor> factors) {
        for (Factor factor : factors) {
            if (factor instanceof DeterministicFactor) {
                DeterministicFactor dFactor = (DeterministicFactor) factor;
                Fraction factorG = dFactor.getAssignedExpression(); //todo in future this may become a piecewise polynomial...
                String factorV = dFactor.getAssociatedVar();
                for (int i = 0; i < factors.size(); i++) {
                    if (!factors.get(i).equals(factor)) {
                        factors.set(i, factors.get(i).substitution(factorV, factorG));
                    }
                }
            }
        }
    }


    /**
     * @param factors  in this stage it is expected that there should be no 'deterministic var' in the scope of stochastic vars. (scope of 'G' is deterministic vars also only contains stochastic vars)
     * @param evidence the evidence is still a mixture of deterministic/stochastic observed vars however, we only select and deal with the deterministic ones.
     * @return a list NEW deterministic relations by which some stochastic vars are eliminated. i.e., from evidence "e=5" where "x/y = e" new relation "y = x/5" is generated and returned (NOTE that the main result is the way factors are altered).
     */
    private List<DeterministicFactor> reduceDimension(List<Factor> factors, Map<String, Double> evidence/*, GraphicalModel originalGM*/) {
        List<DeterministicFactor> formulaeOfEliminatedVars = new ArrayList<DeterministicFactor>();

        //Getting rid of unobserved deterministic factors since they are already isolated whence useless:
        for (int i = 0; i < factors.size(); i++) {
            Factor factor = factors.get(i);
            if (factor instanceof DeterministicFactor) {
                String detVar = ((DeterministicFactor) factor).getAssociatedVar();
                if (!evidence.keySet().contains(detVar)) {
                    factors.set(i, null);
                }
            }
        }

        //use deterministic evidence...
        for (int i = 0; i < factors.size(); i++) {
            Factor factor = factors.get(i);
            if (factor instanceof DeterministicFactor) {
                DeterministicFactor detFactor = (DeterministicFactor) factor;
                String detVar = detFactor.getAssociatedVar();
                if (evidence.keySet().contains(detVar)) {
//                    observedDeterministicVars.add(detVar);
                    DeterministicFactor solution = detFactor.solve(evidence.get(detVar));

                    formulaeOfEliminatedVars.add(solution); //todo is the order matters?
                    debug("..... *solution* = " + solution);

                    factors.set(i, null); // we have done with this (deterministic) factor

                    for (int j = 0; j < factors.size(); j++) {
                        Factor otherFactor = factors.get(j);
                        if (otherFactor != null) {  // the only reason deterministic factors are also handled is that there may be more than one deterministic evidence.
                            factors.set(j, otherFactor.substitution(solution.associatedVar, solution.getAssignedExpression()));
                        }
                    }

                } else {
                    throw new RuntimeException("I expect that in this phase all deterministic evidence are seen.");
                }
            }
        }

        return formulaeOfEliminatedVars;
    }

    void debug(String str) {
        if (DEBUG) System.out.println(str);
    }

    public SamplerInterface makeSampler(GraphicalModel bn, String[] varsToBeSampledJointly, Map<String, Double> evidence, int minVarLimit, int maxVarLimit) {
        Pair<PiecewiseExpression<Fraction>, List<DeterministicFactor>> jointAndEliminatedStochasticVars =
                makeJointAndEliminatedStochasticVars(bn, Arrays.asList(varsToBeSampledJointly), evidence);
        PiecewiseExpression<Fraction> joint = jointAndEliminatedStochasticVars.getFirstEntry();
        PolynomialFactory factory = joint.getFactory();
        final List<DeterministicFactor> eliminatedStochasticVarFactors = jointAndEliminatedStochasticVars.getSecondEntry();
        final int[] eliminatedVarIndices = new int[eliminatedStochasticVarFactors.size()];
        for (int i = 0; i < eliminatedStochasticVarFactors.size(); i++) {
            DeterministicFactor eliminatedStochasticVarFactor = eliminatedStochasticVarFactors.get(i);
            eliminatedVarIndices[i] = factory.getVarIndex(eliminatedStochasticVarFactor.associatedVar);
        }

        //Sampler:
        final SymbolicFractionGibbsSampler sampler = SymbolicFractionGibbsSampler.makeSampler(joint, minVarLimit, maxVarLimit);  //todo any sampler should be acceptable....

        final Double[] querySample = new Double[varsToBeSampledJointly.length];
        final int[] varsToBeSampledIndexes = new int[varsToBeSampledJointly.length];
        for (int i = 0; i < varsToBeSampledJointly.length; i++) {
            String varToBeSampled = varsToBeSampledJointly[i];
            int varIndex = factory.getVarIndex(varToBeSampled);
            varsToBeSampledIndexes[i] = varIndex;
        }
        return new SamplerInterface() {
            @Override
            public Double[] reusableSample() throws SamplingFailureException {
                Double[] innerSample = sampler.reusableSample().clone(); //todo I am not sure if cloning is necessary

                for (int i = eliminatedVarIndices.length - 1; i >= 0; i--) { // eliminated stochastic vars are considered in an inverse-order since e.g. the last eliminated var does not depend on any eliminated var...
                    int eliminatedVarIndex = eliminatedVarIndices[i];
                    double eval = eliminatedStochasticVarFactors.get(i).getAssignedExpression().evaluate(innerSample);

                    //just for debug
                    if (innerSample[eliminatedVarIndex] != null) throw new RuntimeException();

                    innerSample[eliminatedVarIndex] = eval; //for the other eliminated factors that depend on this...
                }

                for (int i = 0; i < varsToBeSampledIndexes.length; i++) {
                    int varIndex = varsToBeSampledIndexes[i];
                    Double value = innerSample[varIndex];

                    //for debug:
                    if (value == null) throw new RuntimeException();

                    querySample[i] = value;
                }

                return querySample;
            }
        };

    }
}
