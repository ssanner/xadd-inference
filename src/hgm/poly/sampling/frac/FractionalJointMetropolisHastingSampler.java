package hgm.poly.sampling.frac;

import hgm.poly.FactorizedPiecewiseStructure;
import hgm.poly.Fraction;
import hgm.poly.PolynomialFactory;
import hgm.poly.bayesian.AbstractGeneralBayesianGibbsSampler;
import hgm.poly.gm.JointToSampler;
import hgm.poly.gm.JointWrapper;
import hgm.poly.sampling.SamplerInterface;
import hgm.sampling.SamplingFailureException;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 19/08/14
 * Time: 12:04 AM
 */
public class FractionalJointMetropolisHastingSampler implements SamplerInterface {
    public static JointToSampler makeJointToSampler(final double proposalVariance) {
        return new JointToSampler() {
            @Override
//            public SamplerInterface makeSampler(PiecewiseExpression<Fraction> joint, double minLimitForAllVars, double maxLimitForAllVars) {
            public SamplerInterface makeSampler(JointWrapper jwi) {
                return FractionalJointMetropolisHastingSampler.makeSampler(jwi.getJoint(),
                        jwi.getMinLimitForAllVars(), jwi.getMaxLimitForAllVars(), proposalVariance);
            }

            @Override
            public String getName() {
                return "mh";
            }
        };
    }

    public static FractionalJointMetropolisHastingSampler makeSampler(
//            PiecewiseExpression<Fraction> joint,
            FactorizedPiecewiseStructure<Fraction> joint,
            double minForAllVars, double maxForAllVars, double proposalVariance) {
        List<String> jointScopeVars = new ArrayList<String>(joint.getScopeVars());
        int numScopeVars = jointScopeVars.size(); // note: these are not all vars in the factory.
        double[] cVarMins = new double[numScopeVars];
        double[] cVarMaxes = new double[numScopeVars];
        Arrays.fill(cVarMins, minForAllVars);
        Arrays.fill(cVarMaxes, maxForAllVars);
        return new FractionalJointMetropolisHastingSampler(joint, jointScopeVars, cVarMins, cVarMaxes, proposalVariance);
    }

    //............................
    public static final boolean DEBUG = true;
    //    protected static final Random random = new Random();
//    public static final double SAMPLE_ACCURACY = 1E-6;
//    public static final int MAX_ITERATIONS_TO_APPROX_F_INVERSE = 20;
//    public static final int MAX_INITIAL_SAMPLING_TRIAL = 1000000000;    // if the function is not positive, (initial) sample cannot be

    int numScopeVars;

    //    PiecewiseExpression<Fraction> joint;
    FactorizedPiecewiseStructure<Fraction> joint;

    Map<Integer, Double> varIndex2MinMap;
    Map<Integer, Double> varIndex2MaxMap;
    Double[] lastSample;
    int[] scopeVarIndexes;

    PolynomialFactory factory;
    private Map<String, Double> reusableSampleAssignment;

    private double proposalVariance;
//    protected Double[] lastSample;


    public FractionalJointMetropolisHastingSampler(
//            PiecewiseExpression<Fraction> joint,
            FactorizedPiecewiseStructure<Fraction> joint,
            List<String> scopeVars,
            double[] cVarMins, double[] cVarMaxes,
            double proposalVariance) {
        this.joint = joint;

        numScopeVars = scopeVars.size();
        if (numScopeVars != cVarMins.length) throw new RuntimeException("size mismatch");
        if (numScopeVars != cVarMaxes.length) throw new RuntimeException("size mismatch");

        this.factory = joint.getFactory();

        reusableSampleAssignment = new HashMap<String, Double>(numScopeVars);
        for (String scopeVar : scopeVars) {
            reusableSampleAssignment.put(scopeVar, null);
        }

        String[] allVars = factory.getAllVars();
        scopeVarIndexes = new int[numScopeVars];
        for (int i = 0; i < scopeVars.size(); i++) {
            String scopeVar = scopeVars.get(i);
            int scopeVarIndex = factory.getVarIndex(scopeVar);
            scopeVarIndexes[i] = scopeVarIndex;
        }

        varIndex2MaxMap = new HashMap<Integer, Double>(numScopeVars);
        varIndex2MinMap = new HashMap<Integer, Double>(numScopeVars);
        for (int i = 0; i < numScopeVars; i++) {
            varIndex2MinMap.put(scopeVarIndexes[i], cVarMins[i]);
            varIndex2MaxMap.put(scopeVarIndexes[i], cVarMaxes[i]);
        }

        this.lastSample = new Double[allVars.length];
        takeInitialSample(lastSample);//reusableInitialSample;//reusableSample();

//        varToSymbolicIntegralMap = new HashMap<Integer, SymbolicCdfArrayHandler>(scopeVars.size());
//        for (int scopeVarIndex : scopeVarIndexes) {
//            Digester d = new Digester(joint, scopeVarIndex);
//            varToSymbolicIntegralMap.put(scopeVarIndex, d.calcSymbolicIntegral());
//        }

        ///////////////////////////////////////////////////////////////////////////////////////

        double initSampleValue = joint.evaluate(lastSample);
//        debug("initSampleValue (" + Arrays.toString(lastSample) + ") = " + initSampleValue);
        if (initSampleValue <= 0.0) {
            throw new SamplingFailureException("valuation of the initial sample is not positive: " + initSampleValue);
        }

        if (proposalVariance <= 0d) throw new RuntimeException("negative variance!!!");
        this.proposalVariance = proposalVariance;

    }

    /**
     * uniformly sample each variable in the interval between its min and max values and reject the produced sample if its probability is not positive...
     */
    protected void takeInitialSample(Double[] reusableSample) throws SamplingFailureException { //todo: maybe rejection based sampling should be used....
//        int failureCount = 0;
        Double targetValue;
        long startTimeMillis = System.currentTimeMillis();

        do {
            if (System.currentTimeMillis() - startTimeMillis > MAX_WAITING_MILLIS_TO_TAKE_INITIAL_SAMPLE) {
//            if (failureCount++ > MAX_INITIAL_SAMPLING_TRIAL) {
                throw new SamplingFailureException("Unable to take initial sample after " + MAX_WAITING_MILLIS_TO_TAKE_INITIAL_SAMPLE / 1000 + " seconds");
            }

            for (int i = 0; i < numScopeVars; i++) {
                int scopeVarIndex = scopeVarIndexes[i];
                double minVarValue = varIndex2MinMap.get(scopeVarIndex);
                double maxVarValue = varIndex2MaxMap.get(scopeVarIndex);
                reusableSample[scopeVarIndex] = AbstractGeneralBayesianGibbsSampler.randomDoubleUniformBetween(minVarValue, maxVarValue);
            }

            targetValue = joint.evaluate(reusableSample);
        } while (targetValue <= 0.0); // a valid sample is found
    }


    protected Double[] jump(Double[] currentSample, int[] scopeVarIndexes, double variance) {
        Double[] newState = new Double[currentSample.length];
        for (int scopeVarIndex : scopeVarIndexes) {
            newState[scopeVarIndex] = AbstractGeneralBayesianGibbsSampler.randomGaussianDouble(currentSample[scopeVarIndex], variance);
        }
        return newState;
    }

    @Override
    public Double[] reusableSample() throws SamplingFailureException {
        Double[] proposalState = jump(lastSample, scopeVarIndexes, proposalVariance);

        double prCurrentState = joint.evaluate(lastSample);
        double prProposalState = joint.evaluate(proposalState);
        double acceptanceRatio = prProposalState / prCurrentState;
        if (acceptanceRatio >= 1) {
            System.arraycopy(proposalState, 0, lastSample, 0, proposalState.length);
            return proposalState;
        }

        if (acceptanceRatio > AbstractGeneralBayesianGibbsSampler.randomDoubleUniformBetween(0, 1)) {
            System.arraycopy(proposalState, 0, lastSample, 0, proposalState.length);
            return proposalState;
        } else {
            Double[] sample = new Double[lastSample.length];
            System.arraycopy(lastSample, 0, sample, 0, lastSample.length);
            return sample;
        }
    }

    public void setProposalVariance(double proposalVariance) {
        this.proposalVariance = proposalVariance;
    }

    private void debug(String string) {
        if (DEBUG) System.out.println(string);
    }

    public Map<String, Double> reusableSampleAssignment() throws SamplingFailureException {
        Double[] sample = reusableSample();
        for (String v : reusableSampleAssignment.keySet()) {
            reusableSampleAssignment.put(v, sample[factory.getVarIndex(v)]);
        }
        return reusableSampleAssignment;
    }

}
