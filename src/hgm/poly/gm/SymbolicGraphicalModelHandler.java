package hgm.poly.gm;

import hgm.asve.Pair;
import hgm.poly.ConstrainedExpression;
import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;
import hgm.poly.PolynomialFactory;
import hgm.poly.sampling.SamplerInterface;
import hgm.sampling.SamplingFailureException;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 23/07/14
 * Time: 5:05 PM
 */
public class SymbolicGraphicalModelHandler {
    public static final boolean DEBUG = false;

    public PiecewiseExpression<Fraction> makeJoint(GraphicalModel gm, List<String> queryVars, Map<String, Double> evidence) {
        Pair<PiecewiseExpression<Fraction>, List<DeterministicFactor>> jointAndEliminatedStochasticVars = makeJointAndEliminatedStochasticVars(gm, queryVars, evidence);
        return jointAndEliminatedStochasticVars.getFirstEntry();
    }

    public Pair<PiecewiseExpression<Fraction> /*joint*/, List<DeterministicFactor> /*eliminatedStochasticVars*/>
    makeJointAndEliminatedStochasticVars(GraphicalModel gm, List<String> queryVars, Map<String, Double> evidence) {

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
        Pair<PiecewiseExpression<Fraction>, List<DeterministicFactor>> resultPair = reduceDimension(factors, evidence);
        PiecewiseExpression<Fraction> finalJoint = resultPair.getFirstEntry();
        List<DeterministicFactor> eliminatedStochasticVars = resultPair.getSecondEntry();
//        debug("\n * \t factors with reduced dimension = " + factors);
        debug("\n * \t finalJoint = " + finalJoint);
//        System.out.println("finalJoint = " + finalJoint);
        debug("\n * \t \t eliminated stochastic vars" + eliminatedStochasticVars);

        //step 4.
//        PiecewiseExpression<Fraction> finalJoint = makeStochasticFactorsJoint(factors);
//        debug("\n * \t finalJoint = " + finalJoint);

//        return new Pair<PiecewiseExpression<Fraction>, List<DeterministicFactor>>(finalJoint, eliminatedStochasticVars);
        return resultPair;
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
     * @param factors  in this stage it is expected that there should be no 'deterministic var' in the scope of stochastic vars. (scope of 'G' in deterministic vars also only contains stochastic vars)
     * @param evidence the evidence is still a mixture of deterministic/stochastic observed vars however, we only select and deal with the deterministic ones.
     * @return a pair of:
     *      1. final dimension reduced joint and
     *      2. list of formula of eliminated stochastic vars (which at present, only consists of a single entry)
     *      i.e. a list of NEW deterministic relations by which some stochastic vars are eliminated. i.e., from evidence "e=5" where "x/y = e" new relation "y = x/5" is generated and returned
     */
    private Pair<PiecewiseExpression<Fraction> /*final result*/, List<DeterministicFactor> /*formulas of the eliminated vars*/> reduceDimension(List<Factor> factors, Map<String, Double> evidence) {
        List<DeterministicFactor> formulaeOfEliminatedVars = new ArrayList<DeterministicFactor>();

        //NEW: I just guarantee that "AT MOST" one deterministic variable is observed since otherwise things will be complicated and may//todo be handled in future
        DeterministicFactor observedDeterministicFactor = null;


        //Getting rid of unobserved deterministic factors since they are already isolated whence useless:
        for (int i = 0; i < factors.size(); i++) {
            Factor factor = factors.get(i);
            if (factor instanceof DeterministicFactor) {
                String detVar = ((DeterministicFactor) factor).getAssociatedVar();
                if (!evidence.keySet().contains(detVar)) {
                    factors.set(i, null);
                } else {
                    //evidence contains the deterministic var:
                    if (observedDeterministicFactor != null)
                        throw new RuntimeException("At the present time, at most one deterministic variable can be observed...");
                    observedDeterministicFactor = (DeterministicFactor) factor;
                    factors.set(i, null); // we have done with this (deterministic) factor.
                }
            }
        }

        PiecewiseExpression<Fraction> joint = makeStochasticFactorsJoint(factors);
        PiecewiseExpression<Fraction> finalResult = null;

        // use deterministic evidence...
        // e.g. evidence x*y=0.3 (more explicitly, z=0.3 where p(z|x*y) = delta[z - x*y]).
        // Here, observed deterministic factor is <z, x*y>
        if (observedDeterministicFactor != null) {
            //e.g. observedDetVar <--> z in the example.
            String observedDetVar = observedDeterministicFactor.getAssociatedVar();

            //e.g. solution = {<x, 0.3/y>}
            List<DeterministicFactor> solutions = observedDeterministicFactor.solve(evidence.get(observedDetVar)); //todo solutions should be distinct for this purpose....

            //all solutions are w.r.t. a same variable. (x in the example)
            String varToBeEliminated = solutions.get(0).getAssociatedVar();

            //NEW: d g / d x1:
            // g is x*y in the example
            Fraction g = observedDeterministicFactor.getAssignedExpression();
            // G' is y in the example
            Fraction gPrime = g.derivativeWrt(varToBeEliminated);

            for (DeterministicFactor solution : solutions) {//at the present time only once...

                formulaeOfEliminatedVars.add(solution); //todo is the order matters?
                debug("..... *solution* = " + solution);

                if (!solution.getAssociatedVar().equals(varToBeEliminated)) throw new RuntimeException("something is gone wrong...");

                // 0.3/y in the example
                Fraction root = solution.getAssignedExpression();

                // {dg(x1, ..., xn)/dx1} |_{x1 <- root_i}
                // in the example, substitution does not occur since here G' is not a function of x.
                Fraction gPrimInstantiated = gPrime.substitute(varToBeEliminated, root);
                Fraction oneDividedByGPrimInstantiated = gPrimInstantiated.returnReciprocal();
                // 1/|G'(X1 = root)| (it is 1/|y| in the example)
                PiecewiseExpression<Fraction> absoluteOneDividedByGPrimInstantiated = oneDividedByGPrimInstantiated.absoluteValue();

                //p(X1 = root, x2, ..., x_n)
                PiecewiseExpression<Fraction> instantiatedJoint = joint.substitute(varToBeEliminated, root);

                PiecewiseExpression<Fraction> multiply = instantiatedJoint.multiply(absoluteOneDividedByGPrimInstantiated);

                if (finalResult==null) {
                     finalResult = multiply;
                } else {
                    finalResult = finalResult.add(multiply);
                }

//                for (int j = 0; j < factors.size(); j++) {
//                    Factor otherFactor = factors.get(j);
//                    if (otherFactor != null) {  // the only reason deterministic factors are also handled is that there may be more than one deterministic evidence.
//                        factors.set(j, otherFactor.substitution(solution.associatedVar, solution.getAssignedExpression()));
//                    }
//                }

            } //end solutions...

//                } else {
//                    throw new RuntimeException("I expect that in this phase all (non-NULL) deterministic vars are in the evidence.");
//                }
        } else {
            finalResult = joint;// joint of stochastic factors
        }
//        }

        return new Pair<PiecewiseExpression<Fraction>, List<DeterministicFactor>>(finalResult, formulaeOfEliminatedVars);
    }

    /*
       AS IT WAS
     * @param factors  in this stage it is expected that there should be no 'deterministic var' in the scope of stochastic vars. (scope of 'G' in deterministic vars also only contains stochastic vars)
     * @param evidence the evidence is still a mixture of deterministic/stochastic observed vars however, we only select and deal with the deterministic ones.
     * @return a list NEW deterministic relations by which some stochastic vars are eliminated. i.e., from evidence "e=5" where "x/y = e" new relation "y = x/5" is generated and returned (NOTE that the main result is the way factors are altered).

    private List<DeterministicFactor> reduceDimension(List<Factor> factors, Map<String, Double> evidence) {
        List<DeterministicFactor> formulaeOfEliminatedVars = new ArrayList<DeterministicFactor>();

        //NEW: I just guarantee that "AT MOST" one deterministic variable is observed since otherwise things will be complicated and may//todo be handled in future
        int observedDeterministicVarsCount = 0;

        //Getting rid of unobserved deterministic factors since they are already isolated whence useless:
        for (int i = 0; i < factors.size(); i++) {
            Factor factor = factors.get(i);
            if (factor instanceof DeterministicFactor) {
                String detVar = ((DeterministicFactor) factor).getAssociatedVar();
                if (!evidence.keySet().contains(detVar)) {
                    factors.set(i, null);
                } else {
                    //evidence contains the deterministic var:
                    observedDeterministicVarsCount++;
                }
            }
        }

        if (observedDeterministicVarsCount>1) throw new RuntimeException("At the moment at most one deterministic variable can be observed...");

        //use deterministic evidence...
        for (int i = 0; i < factors.size(); i++) {
            Factor factor = factors.get(i);
            if (factor instanceof DeterministicFactor) {
                //NOTE: at the moment this should is run at most once since more than one deterministic evidence is not allowed.

                DeterministicFactor detFactor = (DeterministicFactor) factor;
                String detVar = detFactor.getAssociatedVar();
                if (evidence.keySet().contains(detVar)) {
//                    observedDeterministicVars.add(detVar);
                    DeterministicFactor solution = detFactor.solve(evidence.get(detVar)); //todo list of solutions of a same variable

                    formulaeOfEliminatedVars.add(solution); //todo is the order matters?
                    debug("..... *solution* = " + solution);

                    //------------------------------------
                    //NEW: d g / d x1
                    Fraction g = detFactor.getAssignedExpression();
                    String x = solution.getAssociatedVar();
                    Fraction root = solution.getAssignedExpression(); //todo list of roots...
                    Fraction gPrime = g.derivativeWrt(x);

                    // {dg(x1, ..., xn)/dx1} |_{x1 <- root_i}
                    Fraction gPrimInstantiated = gPrime.substitute(x, root);//todo for each root
                    PiecewiseExpression<Fraction> absDerivative = gPrimInstantiated.absoluteValue();
                    //-------------------------------------

                    factors.set(i, null); // we have done with this (deterministic) factor

                    for (int j = 0; j < factors.size(); j++) {
                        Factor otherFactor = factors.get(j);
                        if (otherFactor != null) {  // the only reason deterministic factors are also handled is that there may be more than one deterministic evidence.
                            factors.set(j, otherFactor.substitution(solution.associatedVar, solution.getAssignedExpression()));
                        }
                    }

                } else {
                    throw new RuntimeException("I expect that in this phase all (non-NULL) deterministic vars are in the evidence.");
                }
            }
        }

        return formulaeOfEliminatedVars;
    }
     */

    void debug(String str) {
        if (DEBUG) System.out.println(str);
    }

    @Deprecated
    public SamplerInterface makeQuerySampler(GraphicalModel gm,
                                             List<String> varsToBeSampledJointly,
                                             Map<String, Double> evidence, double minVarLimit, double maxVarLimit, JointToSampler joint2sampler) {
        Pair<PiecewiseExpression<Fraction>, List<DeterministicFactor>> jointAndEliminatedStochasticVars =
                makeJointAndEliminatedStochasticVars(gm, varsToBeSampledJointly, evidence);
        PiecewiseExpression<Fraction> joint = jointAndEliminatedStochasticVars.getFirstEntry();

        final List<DeterministicFactor> eliminatedStochasticVarFactors = jointAndEliminatedStochasticVars.getSecondEntry();

        return makeQuerySampler(joint, eliminatedStochasticVarFactors, varsToBeSampledJointly, minVarLimit, maxVarLimit, joint2sampler);
    }

    // if a var to be sampled jointly is eliminated, it will be instantiated via the "eliminated stochastic var factors..."
    // this compensates the effect of dimension reduction
    public SamplerInterface makeCompletedSampler(final RichJointWrapper richJW, JointToSampler joint2sampler) {

        final PiecewiseExpression<Fraction> joint = richJW.getJoint();
        final List<DeterministicFactor> eliminatedStochasticVarFactors = richJW.getEliminatedStochasticVarFactors(); //eliminated stochastic vars have been in the joint but eliminated via dimension reduction
        final List<String> queryVars = richJW.getQueryVars();
        final double minVarLimit = richJW.getMinLimitForAllVars();
        final double maxVarLimit = richJW.getMaxLimitForAllVars();

        final PolynomialFactory factory = joint.getFactory();
        final int[] eliminatedVarIndices = new int[eliminatedStochasticVarFactors.size()];
//        Set<String> eliminatedStochasticVars = new HashSet<String>(eliminatedStochasticVarFactors.size());
        for (int i = 0; i < eliminatedStochasticVarFactors.size(); i++) {
            DeterministicFactor eliminatedStochasticVarFactor = eliminatedStochasticVarFactors.get(i);
            String associatedVar = eliminatedStochasticVarFactor.getAssociatedVar();
            eliminatedVarIndices[i] = factory.getVarIndex(associatedVar);
//            eliminatedStochasticVars.add(associatedVar);
        }

        //Sampler:
        final SamplerInterface sampler = joint2sampler.makeSampler(new JointWrapper(joint, minVarLimit, maxVarLimit));

        Map<String, Double> evidence = richJW.getEvidence();
        final Double[] evidenceArray = new Double[factory.getAllVars().length];

        GraphicalModel gm = richJW.getGraphicalModel();
        Set<String> detVars = new HashSet<String>(gm.allDeterministicVars());

        for (String eVar : evidence.keySet()) {
            if (!detVars.contains(eVar)) { //I could do it for deterministic vars as well, I just did not do so that I can test deterministic observation easier.
                Double eValue = evidence.get(eVar);
                int eIndex = factory.getVarIndex(eVar);
                evidenceArray[eIndex] = eValue;
            }
        }

        //now deterministic vars instantiation given the non-deterministic vars:
        final Map<Integer, Fraction> queriedDeterministicVarsIndex2determinismFreeValue = new HashMap<Integer, Fraction>();//i.e. if p=p1+p2 and p1=m1*v1 etc., then by query p?, p is mapped to m1v1 + m2v2
        for (String var : queryVars) {
            if (detVars.contains(var)) {
                //find the value associated with this var that does not depend on other deterministic variables.
                List<Factor> thisFactorAndAncestors = ((BayesNetGraphicalModel) gm).fetchAssociatedFactorAndAncestorFactors(var); //todo if "only" for deterministic vars, hierarchical structure is allowed, we are not limited to bayes net...
                isolateDeterministicFactors(thisFactorAndAncestors);
                Fraction f = ((DeterministicFactor) thisFactorAndAncestors.get(0)).getAssignedExpression();
                queriedDeterministicVarsIndex2determinismFreeValue.put(factory.getVarIndex(var), f);
            }
        }

        return new SamplerInterface() {
            @Override
            public Double[] reusableSample() throws SamplingFailureException {
                Double[] completedSample = sampler.reusableSample().clone(); //todo I am not sure if cloning is necessary

                for (int i = eliminatedVarIndices.length - 1; i >= 0; i--) { // eliminated stochastic vars are considered in an inverse-order since e.g. the last eliminated var does not depend on any eliminated var...
                    int eliminatedVarIndex = eliminatedVarIndices[i];
                    double eval = eliminatedStochasticVarFactors.get(i).getAssignedExpression().evaluate(completedSample);

                    //just for debug
                    if (completedSample[eliminatedVarIndex] != null) throw new RuntimeException();

                    completedSample[eliminatedVarIndex] = eval; //for the other eliminated factors that depend on this...
                }

                //fill the variables observed by evidence:
                for (int i = 0; i < evidenceArray.length; i++) {
                    if (evidenceArray[i] != null) {
                        if (completedSample[i] != null)
                            throw new RuntimeException(); //just for debug. the evidence vars are instantiated in the join, so should not exist so should be null.
                        completedSample[i] = evidenceArray[i];
                    }

                }

                //valuate deterministic query vars:
                for (Map.Entry<Integer, Fraction> detQVarIndexAndValue : queriedDeterministicVarsIndex2determinismFreeValue.entrySet()) {
                    if (completedSample[detQVarIndexAndValue.getKey()] != null) throw new RuntimeException();//debug
                    completedSample[detQVarIndexAndValue.getKey()] = detQVarIndexAndValue.getValue().evaluate(completedSample);
                }

                return completedSample;
            }
        };
    }

    //This (old) version, the order of variables is changed i.e.:
    // If jointWrapper.getJoint().getFactory().getAllVars() = [m_1 = (3-m_2*V_2)/v_1, m_2, m_t, p_1, p_2, p_t, v_1, v_2, v_t]
    //                 m_1 =?      m_2            m_t                      v_1     v_2    v_t
    // the sample s = [null,       1.11,          null, null, null, null, 2.22,    3.33, null]
    // by query [m_1, v_1, m_2] is transformed to:
    // the sample s = [(3 - 1.11*3.33)/2.22, 2.22, 1.11]
    // Note: due to the variable reordering, the produced sample cannot be fed to the factory....
    @Deprecated
    public SamplerInterface makeQuerySampler(final PiecewiseExpression<Fraction> joint, final List<DeterministicFactor> eliminatedStochasticVarFactors,
                                             List<String> varsToBeSampledJointly /*query*/, double minVarLimit, double maxVarLimit, JointToSampler joint2sampler) {

        final PolynomialFactory factory = joint.getFactory();
        final int[] eliminatedVarIndices = new int[eliminatedStochasticVarFactors.size()];
        for (int i = 0; i < eliminatedStochasticVarFactors.size(); i++) {
            DeterministicFactor eliminatedStochasticVarFactor = eliminatedStochasticVarFactors.get(i);
            eliminatedVarIndices[i] = factory.getVarIndex(eliminatedStochasticVarFactor.associatedVar);
        }

        //Sampler:
        final SamplerInterface sampler = joint2sampler.makeSampler(new JointWrapper(joint, minVarLimit, maxVarLimit));

        final Double[] querySample = new Double[varsToBeSampledJointly.size()];
        final int[] varsToBeSampledIndexes = new int[varsToBeSampledJointly.size()];
        for (int i = 0; i < varsToBeSampledJointly.size(); i++) {
            String varToBeSampled = varsToBeSampledJointly.get(i);
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
                    if (value == null) throw new RuntimeException("null value for variable " +
                            factory.getAllVars()[varIndex] + ". (innerSample: " + Arrays.toString(innerSample) + " & all vars: " + Arrays.toString(factory.getAllVars()) + ")\n Possibly, a queried var is in evidence!");

                    querySample[i] = value;
                }

                return querySample;
            }
        };
    }
}
