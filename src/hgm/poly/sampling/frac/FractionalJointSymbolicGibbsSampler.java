package hgm.poly.sampling.frac;

import hgm.poly.*;
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
public class FractionalJointSymbolicGibbsSampler implements SamplerInterface {

    public static JointToSampler makeJointToSampler() {
        return new JointToSampler() {
            @Override
//            public SamplerInterface makeSampler(PiecewiseExpression<Fraction> joint, double minLimitForAllVars, double maxLimitForAllVars) {
            public SamplerInterface makeSampler(JointWrapper jwi) {
                return FractionalJointSymbolicGibbsSampler.makeSampler(jwi.getJoint(), jwi.getMinLimitForAllVars(), jwi.getMaxLimitForAllVars());
            }

            @Override
            public String getName() {
                return "symbolic.gibbs";
            }
        };


    }

    public static FractionalJointSymbolicGibbsSampler makeSampler(
//            PiecewiseExpression<Fraction> joint,
            FactorizedPiecewiseStructure<Fraction> joint,
                                                                  double minForAllVars, double maxForAllVars/*, Double[] reusableInitialSample*/) {
        List<String> jointScopeVars = new ArrayList<String>(joint.getScopeVars());
        int numScopeVars = jointScopeVars.size(); // note: these are not all vars in the factory.
        double[] cVarMins = new double[numScopeVars];
        double[] cVarMaxes = new double[numScopeVars];
        Arrays.fill(cVarMins, minForAllVars);
        Arrays.fill(cVarMaxes, maxForAllVars);
        return new FractionalJointSymbolicGibbsSampler(joint, jointScopeVars, cVarMins, cVarMaxes/*, reusableInitialSample*/);
    }

    //..................

    public static final boolean DEBUG = false;//false;
    protected static final Random random = new Random();
    public static final double SAMPLE_ACCURACY = 1E-9;//1E-6;
    public static final int MAX_ITERATIONS_TO_APPROX_F_INVERSE = 45;//30;
    //    public static final int MAX_INITIAL_SAMPLING_TRIAL = 100000000;    // if the function is not positive, (initial) sample cannot be
    int numScopeVars;

//    PiecewiseExpression<Fraction> joint;
    FactorizedPiecewiseStructure<Fraction> joint;
    Map<Integer/*var Index*/, SymbolicCdfArrayHandler> varToSymbolicIntegralMap;
    Map<Integer, Double> varIndex2MinMap;
    Map<Integer, Double> varIndex2MaxMap;
    Double[] reusableSample;
    int[] scopeVarIndexes;

    PolynomialFactory factory;
    private Map<String, Double> reusableSampleAssignment;


    public FractionalJointSymbolicGibbsSampler(
//            PiecewiseExpression<Fraction> joint,
            FactorizedPiecewiseStructure<Fraction> joint,
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

        varToSymbolicIntegralMap = new HashMap<Integer, SymbolicCdfArrayHandler>(scopeVars.size());
        for (int scopeVarIndex : scopeVarIndexes) {
            Digester d = new Digester(joint, scopeVarIndex);
            varToSymbolicIntegralMap.put(scopeVarIndex, d.calcSymbolicIntegral());
        }

        ///////////////////////////////////////////////////////////////////////////////////////

//        if (reusableSample != null) {
//            if (reusableSample.length != numScopeVars)
//                throw new RuntimeException("length mismatch between mins/maxes and the sample vector");

        double initSampleValue = joint.evaluate(reusableSample);
        debug("initSampleValue (" + Arrays.toString(reusableSample) + ") = " + initSampleValue);
        if (initSampleValue <= 0.0) {//todo maybe the XADD is 0
            throw new SamplingFailureException("valuation of the initial sample is not positive: " + initSampleValue);
        }
//        }
    }

    public Map<String, Double> reusableSampleAssignment() throws SamplingFailureException {
        reusableSample = reusableSample();
        for (String v : reusableSampleAssignment.keySet()) {
            reusableSampleAssignment.put(v, reusableSample[factory.getVarIndex(v)]);
        }
        return reusableSampleAssignment;
    }

    int zeroReusableSamplesCount = 0;

    @Override
    public Double[] reusableSample() throws SamplingFailureException {
//        if (reusableSample == null) { // (no sample is taken yet)
//            reusableSample = takeInitialSample();// initialization phase:
//            return reusableSample;
//        }

        reusableSample = professionalSample(reusableSample);
        debug("reusableSample = " + Arrays.toString(reusableSample));

        //just for debug....
        if (joint.evaluate(reusableSample) <= 0) {
//            throw new SamplingFailureException("evaluation of " + Arrays.toString(reusableSample) + " is " + gph.evaluate(reusableSample));
            zeroReusableSamplesCount++;
            System.err.println(zeroReusableSamplesCount + ".\t evaluation of " + Arrays.toString(reusableSample) + " is " + joint.evaluate(reusableSample));
            takeInitialSample(reusableSample);
            debug("reusableSample (by init sample)= " + Arrays.toString(reusableSample));
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

        SymbolicCdfArrayHandler symbolicCDFListHandler = varToSymbolicIntegralMap.get(varIndexToBeSampled);

        OneDimFunction varCDF = symbolicCDFListHandler.instantiate(reusableVarAssign);

        double s = takeSampleFrom1DFunc(varCDF, minVarValue, maxVarValue);

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
                e.printStackTrace();
                if (DEBUG) {
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
//            if (failureCount++ > MAX_INITIAL_SAMPLING_TRIAL) {
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


    public static double takeSampleFrom1DFunc(OneDimFunction varCDF, double minVarValue, double maxVarValue) throws FatalSamplingException {
        Double cdfInfinity = varCDF.eval(maxVarValue + 0.1);
        double normalizedAccuracy = SAMPLE_ACCURACY * cdfInfinity;

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

            if (approxS == 0) {
                double leftLimit = average - ((high - low) * 1E-7);
                double leftLimitValue = varCDF.eval(leftLimit);
                if (leftLimitValue > 0) {
                    //This should not be the case since varCDF is should not decrease!
                    //The only possibility is that we have fallen in hole between the partitions.
                    average = leftLimit;
                    approxS = leftLimitValue;
                }
            }

            if (approxS < s) {
                low = average;
            } else {
                high = average;
            }

            if (low >= high) {
                System.err.println(low + " should be < " + high);
                throw new RuntimeException(low + " should be < " + high);
            }
        } while ((Math.abs(approxS - s) > normalizedAccuracy) && counter++ < MAX_ITERATIONS_TO_APPROX_F_INVERSE);

//        average = (high + low) / 2.0;
        //for test:
        if (varCDF.eval(average) <= 0) {
//            average = (high + low) / 2.0;
            throw new FatalSamplingException("sampling failure." +
                    "\nvarCDF.eval(average: " + average + ") = " + varCDF.eval(average) +
                    "\nMath.abs(approxS:" + approxS + "\t - s:" + s + ") = " + Math.abs(approxS - s) + " should be < normalizedAccuracy = " + normalizedAccuracy +
                    "\ncounter = " + counter +
                    "\nvarCDF.eval(minVarValue: " + minVarValue + ") = " + varCDF.eval(minVarValue) +
                    "\nvarCDF.eval(maxVarValue: " + maxVarValue + ") = " + varCDF.eval(maxVarValue) +
                    "\nvarCDF.eval(maxVarValue+1000) = " + varCDF.eval(maxVarValue + 1000));
//            System.out.println("Math.abs(approxS:"+approxS+"\t - s:" + s+ ") = " + Math.abs(approxS - s) + " should be < normalizedAccuracy = " + normalizedAccuracy);
//            System.out.println("counter = " + counter);
//            System.out.println("varCDF.eval(minVarValue: " + minVarValue + ") = " + varCDF.eval(minVarValue));
//            System.out.println("varCDF.eval(maxVarValue: " + maxVarValue + ") = " + varCDF.eval(maxVarValue));
//            System.out.println("varCDF.eval(maxVarValue+1000) = " + varCDF.eval(maxVarValue + 1000));
//            System.err.println("___________________________");//throw new RuntimeException("average" + average);
        }

        return average;
    }

    public static double randomDoubleUniformBetween(double min, double max) {
        return random.nextDouble() * (max - min) + min;
    }


}



