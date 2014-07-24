package hgm.poly.sampling;

import hgm.poly.ConstrainedPolynomial;
import hgm.poly.integral.OneDimFunction;
import hgm.poly.integral.OneDimPolynomialIntegral;
import hgm.sampling.SamplingFailureException;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 21/02/14
 * Time: 2:31 PM
 */
public class PolyGibbsSampler implements SamplerInterface {
    private static Random random = new Random();
    public static final double SAMPLE_ACCURACY = 1E-6;
    public static final int MAX_ITERATIONS_TO_APPROX_F_INVERSE = 20;
    public static final int MAX_INITIAL_SAMPLING_TRIAL = 10000;    // if the function is not positive, (initial) sample cannot be
    //    private List<String> allVars;
//    private VarAssignment initialSample;
    protected OneDimPolynomialIntegral integrator;

    Double[] reusableSample;
    double[] cVarMins;
    double[] cVarMaxes;
    int numVars;
    ConstrainedPolynomial cp;


    public static PolyGibbsSampler makeGibbsSampler(ConstrainedPolynomial cp, double minForAllVars, double maxForAllVars, Double[] reusableInitialSample) {
        int varNum = cp.getPolynomialFactory().getAllVars().length;
        double[] cVarMins = new double[varNum];
        double[] cVarMaxes = new double[varNum];
        Arrays.fill(cVarMins, minForAllVars);
        Arrays.fill(cVarMaxes, maxForAllVars);
        return new PolyGibbsSampler(cp, cVarMins, cVarMaxes, reusableInitialSample);
    }

    public PolyGibbsSampler(ConstrainedPolynomial cp, double[] cVarMins, double[] cVarMaxes, Double[] reusableInitialSample) {

        this.cp = cp;

        integrator = new OneDimPolynomialIntegral();

        this.reusableSample = reusableInitialSample;
        this.cVarMins = cVarMins;
        this.cVarMaxes = cVarMaxes;
        numVars = cVarMins.length;
        if (cVarMaxes.length != numVars) throw new RuntimeException("length mismatch between mins and maxes");

        if (reusableSample != null) {
            if (reusableSample.length != numVars) throw new RuntimeException("length mismatch between mins/maxes and the sample vector");
            Double initSampleValue = cp.evaluate(reusableSample);
            if (initSampleValue <= 0.0) {//todo maybe the XADD is 0
                throw new SamplingFailureException("valuation of the initial sample is not positive: " + initSampleValue);
            }
        }
    }

    @Override
    public Double[] reusableSample() throws SamplingFailureException {
        if (reusableSample == null) { // (no sample is taken yet)
            reusableSample = takeInitialSample();// initialization phase:
        } else {
//            for (String bVar : bVars) {
//                throw new RuntimeException("does not work with boolean variables YET....");
//                sampleSingleVar(bVar, reusableVarAssignment);
//            }
            String[] allVars = cp.getPolynomialFactory().getAllVars();
            for (int i=0; i<numVars; i++){
                sampleSingleContinuousVar(allVars[i], i, reusableSample);
            }
        }
        return reusableSample;
    }

    /**
     * uniformly sample each variable in the interval between its min and max values and reject the produced sample if its probability is not positive...
     */
    private Double[] takeInitialSample() throws SamplingFailureException {

        int failureCount = 0;

        Double[] initSample = new Double[numVars];
        Double targetValue;

        do {
            if (failureCount++ > MAX_INITIAL_SAMPLING_TRIAL)
                throw new SamplingFailureException("Unable to take initial sample");
//            for (String bVar : bVars) {
//                boolAssign.put(bVar, super.randomBoolean());
//            }
            for (int i=0; i<numVars; i++) {
                double minVarValue = cVarMins[i];
                double maxVarValue = cVarMaxes[i];
                initSample[i] = randomDoubleUniformBetween(minVarValue, maxVarValue);
            }

            targetValue = cp.evaluate(initSample);
        } while (targetValue <= 0.0); // a valid sample is found

        return initSample;
    }

    protected void sampleSingleContinuousVar(String varToBeSampled, int varIndexToBeSampled, Double[] reusableVarAssign) { //todo: only var-index should be enough

        //todo: important: it is wrong to sample from all variables in the factory.... One should only sample from the variables in the cp
        OneDimFunction varCDF = makeCumulativeDistributionFunction(cp, varToBeSampled, reusableVarAssign);

        double maxVarValue = cVarMaxes[varIndexToBeSampled];
        double minVarValue = cVarMins[varIndexToBeSampled];

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

        // here the sample is stored....
        reusableVarAssign[varIndexToBeSampled] = average;
    }

    //NOTE: only works with continuous vars...
    // returns int_{w=-infty}^{var} (func[var->w]dw) for instantiated function
    public OneDimFunction makeCumulativeDistributionFunction(ConstrainedPolynomial cp, String var, Double[] currentVarAssign) {

        OneDimFunction cdf = integrator.integrate(cp, var, currentVarAssign);

        return cdf;
    }

    protected static double randomDoubleUniformBetween(double min, double max) {
        return random.nextDouble() * (max - min) + min;
    }

    protected static boolean randomBoolean() {
        return random.nextBoolean();
    }

}
