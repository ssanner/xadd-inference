package hgm.poly.sampling.frac;

import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;
import hgm.poly.PolynomialFactory;
import hgm.poly.gm.JointToSampler;
import hgm.poly.gm.JointWrapper;
import hgm.poly.integral.OneDimFunction;
import hgm.poly.integral.frac.Digester;
import hgm.poly.pref.FatalSamplingException;
import hgm.poly.pref.SymbolicCdfArrayHandler;
import hgm.poly.sampling.SamplerInterface;
import hgm.sampling.SamplingFailureException;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 5/08/14
 * Time: 9:23 AM
 */
public class FractionalJointBaselineGibbsSampler implements SamplerInterface {

    public static JointToSampler makeJointToSampler() {
        return new JointToSampler() {
            @Override
//            public SamplerInterface makeSampler(PiecewiseExpression<Fraction> joint, double minLimitForAllVars, double maxLimitForAllVars) {
            public SamplerInterface makeSampler(JointWrapper jwi) {
                return FractionalJointBaselineGibbsSampler.makeSampler(jwi.getJoint(), jwi.getMinLimitForAllVars(), jwi.getMaxLimitForAllVars());
            }

            @Override
            public String getName() {
                return "baseline.gibbs";
            }
        };
    }

    public static FractionalJointBaselineGibbsSampler makeSampler(PiecewiseExpression<Fraction> joint,
                                                                  double minForAllVars, double maxForAllVars/*, Double[] reusableInitialSample*/) {
        List<String> jointScopeVars = new ArrayList<String>(joint.getScopeVars());
        int numScopeVars = jointScopeVars.size(); // note: these are not all vars in the factory.
        double[] cVarMins = new double[numScopeVars];
        double[] cVarMaxes = new double[numScopeVars];
        Arrays.fill(cVarMins, minForAllVars);
        Arrays.fill(cVarMaxes, maxForAllVars);
        return new FractionalJointBaselineGibbsSampler(joint, jointScopeVars, cVarMins, cVarMaxes/*, reusableInitialSample*/);
    }

    //..................

    public static final boolean DEBUG = true;
    protected static final Random random = new Random();
//    public static final double SAMPLE_ACCURACY = 1E-6;
//    public static final int MAX_ITERATIONS_TO_APPROX_F_INVERSE = 20;
//    public static final int MAX_INITIAL_SAMPLING_TRIAL = 10000000;    // if the function is not positive, (initial) sample cannot be
    int numScopeVars;
    PiecewiseExpression<Fraction> joint;
    Map<Integer, Double> varIndex2MinMap;
    Map<Integer, Double> varIndex2MaxMap;
    Double[] reusableSample;
    int[] scopeVarIndexes;

    PolynomialFactory factory;
    private Map<String, Double> reusableSampleAssignment;


    public FractionalJointBaselineGibbsSampler(PiecewiseExpression<Fraction> joint,
                                               List<String> scopeVars,
                                               double[] cVarMins, double[] cVarMaxes/*, Double[] reusableInitialSample*/) {
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

        this.reusableSample = new Double[allVars.length];
        takeInitialSample(reusableSample);//reusableInitialSample;//reusableSample();

        double initSampleValue = joint.evaluate(reusableSample);
//        debug("initSampleValue (" + Arrays.toString(reusableSample) + ") = " + initSampleValue);
        if (initSampleValue <= 0.0) {//todo maybe the XADD is 0
            throw new SamplingFailureException("valuation of the initial sample is not positive: " + initSampleValue);
        }
    }

    public Map<String, Double> reusableSampleAssignment() throws SamplingFailureException {
        reusableSample = reusableSample();
        for (String v : reusableSampleAssignment.keySet()) {
            reusableSampleAssignment.put(v, reusableSample[factory.getVarIndex(v)]);
        }
        return reusableSampleAssignment;
    }

    @Override
    public Double[] reusableSample() throws SamplingFailureException {
//        if (reusableSample == null) { // (no sample is taken yet)
//            reusableSample = takeInitialSample();// initialization phase:
//            return reusableSample;
//        }

        reusableSample = professionalSample(reusableSample);

        //just for debug....
        if (joint.evaluate(reusableSample) <= 0) {
//            throw new SamplingFailureException("evaluation of " + Arrays.toString(reusableSample) + " is " + gph.evaluate(reusableSample));
            System.err.println("evaluation of " + Arrays.toString(reusableSample) + " is " + joint.evaluate(reusableSample));
            takeInitialSample(reusableSample);
            return reusableSample();
        }


        return reusableSample;
    }

    private void debug(String string) {
        if (DEBUG) System.out.println(string);
    }

    protected void sampleSingleContinuousVar(int varIndexToBeSampled, Double[] reusableVarAssign) throws FatalSamplingException {
        double maxVarValue = varIndex2MaxMap.get(varIndexToBeSampled);
        double minVarValue = varIndex2MinMap.get(varIndexToBeSampled);

//        SymbolicCdfArrayHandler symbolicCDFListHandler = varToSymbolicIntegralMap.get(varIndexToBeSampled);
//        OneDimFunction varCDF = symbolicCDFListHandler.instantiate(reusableVarAssign);
//        varToSymbolicIntegralMap.put(scopeVarIndex, d.calcSymbolicIntegral());

        reusableVarAssign[varIndexToBeSampled] = null;
        PiecewiseExpression<Fraction> univarJoint = joint.substitute(reusableVarAssign);
        //debug:
        if (univarJoint.getScopeVars().size()!=1) throw new SamplingFailureException("at this stage joint should be univariate but it is not! Joint = " + joint);
        Digester d = new Digester(joint, varIndexToBeSampled);
        SymbolicCdfArrayHandler symbolicCdfArrayHandler = d.calcSymbolicIntegral();
        OneDimFunction varCDF = symbolicCdfArrayHandler.instantiate(reusableVarAssign);

        double s = FractionalJointSymbolicGibbsSampler.takeSampleFrom1DFunc(varCDF, minVarValue, maxVarValue);

        // here the sample is stored....
        reusableVarAssign[varIndexToBeSampled] = s;
    }


    public int debugNumUnsuccessfulSamplings = 0;

    //override if not Gibbs variations...
    protected Double[] professionalSample(Double[] reusableSample) {

        for (int i = 0; i < numScopeVars; i++) {
            try {
                sampleSingleContinuousVar(/*allVars[i], */scopeVarIndexes[i], reusableSample);
            } catch (FatalSamplingException e) {
                debugNumUnsuccessfulSamplings++;
                if (DEBUG) {
                    e.printStackTrace();
                    System.err.println("" +
                            "#unsuccessful samples: " + debugNumUnsuccessfulSamplings);

                }
                takeInitialSample(reusableSample);

                return reusableSample();  //since if it is directly returned, the samples tend towards a uniform distribution
            }
        }

//        if (filledWithZeros(reusableSample)) {
//            throw new RuntimeException("Hmm! all 0(?) Isn't this suspicious");
            /*if (DEBUG) {
                System.err.println("[" + debugNumUnsuccessfulSamplings + "] \t sample= " + Arrays.toString(reusableSample) + "!");
            }
            if (prevSample == null) {
                takeInitialSample(reusableSample);
                return professionalSample(reusableSample);
            } else {
                System.arraycopy(prevSample, 0, reusableSample, 0, reusableSample.length);
            }

            debugNumUnsuccessfulSamplings++;
            return reusableSample;*/
//        }

        return reusableSample;
    }

    public static boolean filledWithZeros(Double[] arr) {
        for (Double d : arr) {
            if (d != null && d != 0) return false;
        }
        return true;
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
                throw new SamplingFailureException("Unable to take initial sample after " + MAX_WAITING_MILLIS_TO_TAKE_INITIAL_SAMPLE / 1000 + " seconds");
            }
//            if (failureCount++ > MAX_INITIAL_SAMPLING_TRIAL)
//                throw new SamplingFailureException("Unable to take initial sample");

            for (int i = 0; i < numScopeVars; i++) {
                int scopeVarIndex = scopeVarIndexes[i];
                double minVarValue = varIndex2MinMap.get(scopeVarIndex);
                double maxVarValue = varIndex2MaxMap.get(scopeVarIndex);
                reusableSample[scopeVarIndex] = randomDoubleUniformBetween(minVarValue, maxVarValue);
            }

            targetValue = joint.evaluate(reusableSample);
        } while (targetValue <= 0.0); // a valid sample is found
    }


/*
    protected double takeSampleFrom1DFunc(OneDimFunction varCDF, double minVarValue, double maxVarValue) {
        Double cdfInfinity = varCDF.eval(maxVarValue + 0.1);

        double s = randomDoubleUniformBetween(0.0, cdfInfinity);
        //now: F^{-1}(X=s) should be computed:
        //Binary search algorithm:
        double low = minVarValue;
        double high = maxVarValue;
        double approxS;
        double average;

        //NOTE: the algorithm only works since there is no delta (i.e. no discontinuity in the CDF)
        //however, counter is added in case XADD rounding causes problems
        int counter = 0;
        do {
            average = (high + low) / 2.0;

//            reusableVarAssign.assignContinuousVariable(varToBeSampled, average);  //here the sample is stored
//            approxS = context.evaluate(cdfId,
//                    reusableVarAssign.getBooleanVarAssign(), reusableVarAssign.getContinuousVarAssign());

            approxS = varCDF.eval(average);

            if (approxS < s) {
                low = average;
            } else {
                high = average;
            }


            if (low >= high) System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + low + " should be < " + high);

        } while ((Math.abs(approxS - s) > SAMPLE_ACCURACY) && counter++ < MAX_ITERATIONS_TO_APPROX_F_INVERSE);

        return average;
    }
*/


    public static double randomDoubleUniformBetween(double min, double max) {
        return random.nextDouble() * (max - min) + min;
    }


}



