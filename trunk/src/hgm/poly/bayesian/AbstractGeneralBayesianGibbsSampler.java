package hgm.poly.bayesian;

import hgm.poly.ConstrainedExpression;
import hgm.poly.PiecewiseExpression;
import hgm.poly.Polynomial;
import hgm.poly.PolynomialFactory;
import hgm.poly.integral.OneDimFunction;
import hgm.poly.integral.OneDimPolynomialIntegral;
import hgm.poly.pref.FatalSamplingException;
import hgm.poly.sampling.SamplerInterface;
import hgm.sampling.SamplingFailureException;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by Hadi Afshar.
 * Date: 11/03/14
 * Time: 9:05 PM
 */
public abstract class AbstractGeneralBayesianGibbsSampler implements SamplerInterface {
    protected static final Random random = new Random();
    public static final double SAMPLE_ACCURACY = 1E-6;
    public static final int MAX_ITERATIONS_TO_APPROX_F_INVERSE = 20;
    public static final int MAX_INITIAL_SAMPLING_TRIAL = 100000000;    // if the function is not positive, (initial) sample cannot be
    //    private List<String> allVars;
//    private VarAssignment initialSample;
    protected OneDimPolynomialIntegral integrator;

    static boolean DEBUG = true;

    Double[] reusableSample;
    double[] cVarMins;
    double[] cVarMaxes;
    int numVars;
    GeneralBayesianPosteriorHandler gph;

    Double[] absoluteBestSample;
    double absoluteBestTarget = -1;

    Double[] prevSample = null;
    public double perplex = 0.0; //0.002;

    public AbstractGeneralBayesianGibbsSampler(GeneralBayesianPosteriorHandler gph/*, double[] cVarMins, double[] cVarMaxes*/, Double[] reusableInitialSample) {

        this.gph = gph;
        this.cVarMins = gph.getPriorHandler().getLowerBoundsPerDim();
        this.cVarMaxes = gph.getPriorHandler().getUpperBoundsPerDim();

        integrator = new OneDimPolynomialIntegral();

        this.reusableSample = reusableInitialSample;
//        this.cVarMins = cVarMins;
//        this.cVarMaxes = cVarMaxes;
        numVars = cVarMins.length;
        if (cVarMaxes.length != numVars) throw new RuntimeException("length mismatch between mins and maxes");

        absoluteBestSample = new Double[numVars];

        if (reusableSample != null) {
            System.arraycopy(reusableInitialSample, 0, absoluteBestSample, 0, numVars);

            if (reusableSample.length != numVars)
                throw new RuntimeException("length mismatch between mins/maxes and the sample vector");
            double initSampleValue = gph.evaluate(reusableSample);
            System.out.println("initSampleValue (" + Arrays.toString(reusableSample) + ") = " + initSampleValue);
            if (initSampleValue <= 0.0) {//todo maybe the XADD is 0
                throw new SamplingFailureException("valuation of the initial sample is not positive: " + initSampleValue);
            }
        }
    }

    public PolynomialFactory getFactory() {
        return gph.getPolynomialFactory();
    }

    @Override
    public Double[] reusableSample() throws SamplingFailureException {
        if (reusableSample == null) { // (no sample is taken yet)
            reusableSample = takeInitialSample();// initialization phase:
            return reusableSample;
        }

        reusableSample = professionalSample(reusableSample);

        //just for debug....
        if (gph.evaluate(reusableSample) <= 0) {
//            throw new SamplingFailureException("evaluation of " + Arrays.toString(reusableSample) + " is " + gph.evaluate(reusableSample));
            System.err.println("evaluation of " + Arrays.toString(reusableSample) + " is " + gph.evaluate(reusableSample));
            reusableSample = takeInitialSample();
            return reusableSample();
        }


        return reusableSample;
    }

    public int debugNumUnsuccessfulSamplings = 0;

    //override if not Gibbs variations...
    protected Double[] professionalSample(Double[] reusableSample) {
        if (prevSample != null && random.nextDouble() < perplex) {
            System.arraycopy(prevSample, 0, reusableSample, 0, reusableSample.length);
            System.out.print(".");
            return reusableSample;
        }

//        String[] allVars = gph.getPolynomialFactory().getAllVars();
        for (int i = 0; i < numVars; i++) {
//            double vBeforeSampling = reusableSample[i];
            try {
                sampleSingleContinuousVar(/*allVars[i], */i, reusableSample);
            } catch (FatalSamplingException e) {
                debugNumUnsuccessfulSamplings++;
                if (DEBUG) {
                    e.printStackTrace();
                    System.err.println("" +
                            "#unsuccessful samples: " + debugNumUnsuccessfulSamplings);

                }
                this.reusableSample = takeInitialSample();

                return reusableSample();  //since if it is directly returned, the samples tend towards a uniform distribution
            }
//            if (DEBUG) System.out.println("reusableSample [" + i + "]= " + Arrays.toString(reusableSample));

        }
//        if (DEBUG) System.out.println("* reusableSample = " + Arrays.toString(reusableSample));

        if (filledWithZeros(reusableSample)) {
            if (DEBUG) {
                System.err.println("[" + debugNumUnsuccessfulSamplings + "] \t sample= " + Arrays.toString(reusableSample) + "!");
            }
            if (prevSample == null) {
                reusableSample = takeInitialSample();
                return professionalSample(reusableSample);
            } else {
                System.arraycopy(prevSample, 0, reusableSample, 0, reusableSample.length);
            }

            debugNumUnsuccessfulSamplings++;
            return reusableSample;
        }
        return reusableSample;
    }

    public static boolean filledWithZeros(Double[] arr) {
        for (Double d : arr) {
            if (d != 0) return false;
        }
        return true;
    }

    /**
     * uniformly sample each variable in the interval between its min and max values and reject the produced sample if its probability is not positive...
     */
    protected Double[] takeInitialSample() throws SamplingFailureException { //todo: maybe rejection based sampling should be used....
//        boolean alternativeMethod = false;
//        if (alternativeMethod)
//            return clvAlternativeInitSample();
//        else {
            int failureCount = 0;

            Double[] initSample = new Double[numVars];
            Double targetValue;

            do {
                if (failureCount++ > MAX_INITIAL_SAMPLING_TRIAL)
                    throw new SamplingFailureException("Unable to take initial sample");
/////            for (String bVar : bVars) {
/////                boolAssign.put(bVar, super.randomBoolean());
/////            }
                for (int i = 0; i < numVars; i++) {
                    double minVarValue = cVarMins[i];
                    double maxVarValue = cVarMaxes[i];
                    initSample[i] = randomDoubleUniformBetween(minVarValue, maxVarValue);
                }

                targetValue = gph.evaluate(initSample);
            } while (targetValue <= 0.0); // a valid sample is found

            return initSample;
//        }
    }

    abstract protected void sampleSingleContinuousVar(/*String varToBeSampled, */int varIndexToBeSampled, Double[] reusableVarAssign) throws FatalSamplingException;//todo: only var-index should be enough

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
        } while ((Math.abs(approxS - s) > SAMPLE_ACCURACY) && counter++ < MAX_ITERATIONS_TO_APPROX_F_INVERSE);

        return average;
    }

    public void makeAndAddCumulativeDistributionFunctionsToList(PiecewiseExpression<Polynomial> pp, int varIndex, Double[] currentVarAssign, List<OneDimFunction> cdfList) {
        for (ConstrainedExpression cp : pp.getCases()) {

            // returns int_{w=-infty}^{var} (func[var->w]dw) for instantiated function:
            OneDimFunction cdf = integrator.integrate(cp, varIndex, currentVarAssign);
            if (!cdf.equals(OneDimFunction.ZERO_1D_FUNCTION)) {
                cdfList.add(cdf);
            }
        }
    }

    //NOTE: only works with continuous vars...
    // returns int_{w=-infty}^{var} (func[var->w]dw) for instantiated function
//    public OneDimFunction makeCumulativeDistributionFunction(ConstrainedPolynomial cp, String var, Double[] currentVarAssign) {
//        OneDimFunction cdf = integrator.integrate(cp, var, currentVarAssign);
//        return cdf;
//    }

    public static double randomDoubleUniformBetween(double min, double max) {
        return random.nextDouble() * (max - min) + min;
    }

    public static Double randomGaussianDouble(Double mean, double variance) {
        return mean + random.nextGaussian() * variance;
    }


/*
    @Deprecated
    Double[] clvAlternativeInitSample() {
        Double[] bestTakenSample = new Double[numVars];
        double bestSeenTarget = -1;
        Double[] sample = new Double[numVars];
        double targetValue;

        for (; ; ) {
            for (int n = 0; n < 1000; n++) {

                //take a sample
                for (int i = 0; i < numVars; i++) {
                    double minVarValue = cVarMins[i];
                    double maxVarValue = cVarMaxes[i];
                    sample[i] = randomDoubleUniformBetween(minVarValue, maxVarValue);
                }
                targetValue = gph.evaluate(sample);
                if (targetValue > bestSeenTarget) {

                    //bestTakenSample = sample.clone();
                    System.arraycopy(sample, 0, bestTakenSample, 0, numVars);
                    bestSeenTarget = targetValue;
                }

            }

            if (absoluteBestTarget < bestSeenTarget) {
                absoluteBestTarget = bestSeenTarget;
                System.arraycopy(bestTakenSample, 0, absoluteBestSample, 0, numVars);
            }

            if (bestSeenTarget > 0) {
                return bestTakenSample;
//                double p = bestSeenTarget / bestSeenTarget + absoluteBestTarget;
//                return randomDoubleUniformBetween(0, 1) < p?
//                        bestTakenSample
//                        :
//                        absoluteBestSample;
            }
            if (absoluteBestTarget > 0) return absoluteBestSample;
        }
    }
*/

    public static Double[] cloneSample(Double[] reusableSample) {
        Double[] clonedSample = new Double[reusableSample.length];
        System.arraycopy(reusableSample, 0, clonedSample, 0, reusableSample.length);
        return clonedSample;
    }

}

