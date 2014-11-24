package hgm.poly.sampling.frac;

import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;
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
public class FractionalJointRejectionSampler implements SamplerInterface{
    public static boolean DO_NOT_PRODUCE_ERROR_FOR_VERY_SMALL_NEGATIVE_NUMBERS = true;
    public static double NEGLIGIBLE_NEGATIVE_VALUE_THRESHOLD = -10E-15; // for values less than this, error will be generated anyway...

    public static JointToSampler makeJointToSampler(final double envelopeCoef){
        return new JointToSampler() {
            @Override
//            public SamplerInterface makeSampler(PiecewiseExpression<Fraction> joint, double minLimitForAllVars, double maxLimitForAllVars) {
            public SamplerInterface makeSampler(JointWrapper jwi) {
                return FractionalJointRejectionSampler.makeSampler(jwi.getJoint(), jwi.getMinLimitForAllVars(), jwi.getMaxLimitForAllVars(), envelopeCoef);
            }

            @Override
            public String getName() {
                return "rej";
            }
        };
    }

    public static FractionalJointRejectionSampler makeSampler(PiecewiseExpression<Fraction> joint,
                                                                  double minForAllVars, double maxForAllVars,
                                                                  double envelopeCoef) {
        List<String> jointScopeVars = new ArrayList<String>(joint.getScopeVars());
        int numScopeVars = jointScopeVars.size(); // note: these are not all vars in the factory.
        double[] cVarMins = new double[numScopeVars];
        double[] cVarMaxes = new double[numScopeVars];
        Arrays.fill(cVarMins, minForAllVars);
        Arrays.fill(cVarMaxes, maxForAllVars);
        return new FractionalJointRejectionSampler(joint, jointScopeVars, cVarMins, cVarMaxes, envelopeCoef);
    }

    //............................
    public static final boolean DEBUG = true;
//    public static final int MAX_INITIAL_SAMPLING_TRIAL = 100000000;    // if the function is not positive, (initial) sample cannot be
    public static final long MAX_WAITING_MILLI_SECONDS_TO_TAKE_A_SAMPLE = 1000 * 20;
    int numScopeVars;
    PiecewiseExpression<Fraction> joint;
    Map<Integer, Double> varIndex2MinMap;
    Map<Integer, Double> varIndex2MaxMap;
    int[] scopeVarIndexes;

    PolynomialFactory factory;
    private Map<String, Double> reusableSampleAssignment;

    private double envelope;
    private int sampleArraySize;


    public FractionalJointRejectionSampler(PiecewiseExpression<Fraction> joint,
                                           List<String> scopeVars,
                                           double[] cVarMins, double[] cVarMaxes,
                                           double envelope) {
        this.joint = joint;

        numScopeVars = scopeVars.size();
        if (numScopeVars != cVarMins.length) throw new RuntimeException("size mismatch");
        if (numScopeVars != cVarMaxes.length) throw new RuntimeException("size mismatch");

        this.factory = joint.getFactory();

        reusableSampleAssignment = new HashMap<String, Double>(numScopeVars);
        for (String scopeVar : scopeVars) {
            reusableSampleAssignment.put(scopeVar, null);
        }

        this.sampleArraySize = factory.getAllVars().length;

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

        if (envelope <= 0d) throw new RuntimeException("negative envelope!!!");
        this.envelope = envelope;
    }

    public Double[] reusableSample() throws SamplingFailureException {

        long startTimeMillis = System.currentTimeMillis();

//        int failureCount = 0;

        Double[] sample = new Double[sampleArraySize];

        for(;;) {
            if (System.currentTimeMillis() - startTimeMillis > MAX_WAITING_MILLI_SECONDS_TO_TAKE_A_SAMPLE) {
//            if (failureCount++ > MAX_INITIAL_SAMPLING_TRIAL) {
                throw new SamplingFailureException("Unable to take initial sample after " + MAX_WAITING_MILLI_SECONDS_TO_TAKE_A_SAMPLE/1000 + " seconds");
            }

            for (int scopeVarIndex : scopeVarIndexes){
                double minVarValue = varIndex2MinMap.get(scopeVarIndex);
                double maxVarValue = varIndex2MaxMap.get(scopeVarIndex);
                sample[scopeVarIndex] = AbstractGeneralBayesianGibbsSampler.randomDoubleUniformBetween(minVarValue, maxVarValue);
            }

            double v = joint.evaluate(sample);
            if (v < 0) {
                if (DO_NOT_PRODUCE_ERROR_FOR_VERY_SMALL_NEGATIVE_NUMBERS && v > NEGLIGIBLE_NEGATIVE_VALUE_THRESHOLD) v=0;
                else throw new SamplingFailureException("negative target value: " + v + "for sample: " + Arrays.toString(sample) +
                        "\n in joint: " + joint + "\n with vars" + joint.getScopeVars());
            }

            double pr = v / envelope;
            if (pr > 1)
                throw new RuntimeException("sampled value: f" + Arrays.toString(sample) + " = " + v + "\t is greater than envelope " + envelope);

            if (pr >= AbstractGeneralBayesianGibbsSampler.randomDoubleUniformBetween(0, 1)) {
                return sample;
            }
        }
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
