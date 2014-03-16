package hgm.poly.pref;

import hgm.poly.ConstrainedPolynomial;
import hgm.poly.integral.OneDimFunction;
import hgm.poly.integral.OneDimPolynomialIntegral;
import hgm.poly.sampling.SamplerInterface;
import hgm.poly.vis.FunctionVisualizer;
import hgm.sampling.SamplingFailureException;
import hgm.sampling.XaddSampler;
import hgm.utils.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by Hadi Afshar.
 * Date: 11/03/14
 * Time: 9:05 PM
 */
public class GatedPolytopesSampler implements SamplerInterface {
    private static Random random = new Random();
    public static final double SAMPLE_ACCURACY = 1E-6;
    public static final int MAX_ITERATIONS_TO_APPROX_F_INVERSE = 20;
    public static final int MAX_INITIAL_SAMPLING_TRIAL = 10000;    // if the function is not positive, (initial) sample cannot be
    //    private List<String> allVars;
//    private VarAssignment initialSample;
    protected OneDimPolynomialIntegral integrator;

    static boolean DEBUG = false;

    Double[] reusableSample;
    double[] cVarMins;
    double[] cVarMaxes;
    int numVars;
    GatedPolytopesHandler gph;

    Double[] absoluteBestSample;
    double absoluteBestTarget = -1;


    public static GatedPolytopesSampler makeGibbsSampler(GatedPolytopesHandler gph, double minForAllVars, double maxForAllVars, Double[] reusableInitialSample) {
        int varNum = gph.getPolynomialFactory().getAllVars().length;
        double[] cVarMins = new double[varNum];
        double[] cVarMaxes = new double[varNum];
        Arrays.fill(cVarMins, minForAllVars);
        Arrays.fill(cVarMaxes, maxForAllVars);
        return new GatedPolytopesSampler(gph, cVarMins, cVarMaxes, reusableInitialSample);
    }

    public GatedPolytopesSampler(GatedPolytopesHandler gph, double[] cVarMins, double[] cVarMaxes, Double[] reusableInitialSample) {

        this.gph = gph;

        integrator = new OneDimPolynomialIntegral();

        this.reusableSample = reusableInitialSample;
        this.cVarMins = cVarMins;
        this.cVarMaxes = cVarMaxes;
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

    public Double[] sample() throws SamplingFailureException {
        if (reusableSample == null) { // (no sample is taken yet)
            reusableSample = takeInitialSample();// initialization phase:
        } else {
//            for (String bVar : bVars) {
//                throw new RuntimeException("does not work with boolean variables YET....");
//                sampleSingleVar(bVar, reusableVarAssignment);
//            }

            int prevSampleHashCode = Arrays.hashCode(reusableSample);

            String[] allVars = gph.getPolynomialFactory().getAllVars();
            for (int i = 0; i < numVars; i++) {
                double vBeforeSampling = reusableSample[i];
                sampleSingleContinuousVar(allVars[i], i, reusableSample);
                if (DEBUG) System.out.println("reusableSample [" + i + "]= " + Arrays.toString(reusableSample));

                //todo: is this ok?
//                if (vBeforeSampling == reusableSample[i]) {
//                    reusableSample = takeInitialSample();
//                    return reusableSample;
//                }

            }
            if (DEBUG) System.out.println("* reusableSample = " + Arrays.toString(reusableSample));

            if (filledWithZeros(reusableSample)) {
                reusableSample = takeInitialSample();
            }

            if (prevSampleHashCode == Arrays.hashCode(reusableSample)) {
                if (DEBUG)
                    System.err.println(Arrays.toString(reusableSample) + " is a repeated sample! Do a random jump...");
                reusableSample = takeInitialSample();
            }
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
    private Double[] takeInitialSample() throws SamplingFailureException { //todo: maybe rejection based sampling should be used....

        return cleverInitSample();
        /*int failureCount = 0;

        Double[] initSample = new Double[numVars];
        Double targetValue;

        do {
            if (failureCount++ > MAX_INITIAL_SAMPLING_TRIAL)
                throw new SamplingFailureException("Unable to take initial sample");
//            for (String bVar : bVars) {
//                boolAssign.put(bVar, super.randomBoolean());
//            }
            for (int i = 0; i < numVars; i++) {
                double minVarValue = cVarMins[i];
                double maxVarValue = cVarMaxes[i];
                initSample[i] = randomDoubleUniformBetween(minVarValue, maxVarValue);
            }

            targetValue = gph.evaluate(initSample);
        } while (targetValue <= 0.0); // a valid sample is found

        return initSample;*/
    }

    protected void sampleSingleContinuousVar(String varToBeSampled, int varIndexToBeSampled, Double[] reusableVarAssign) { //todo: only var-index should be enough

        //todo: important: it is wrong to sample from all variables in the factory.... One should only sample from the variables in the cp

        List<Boolean> gateMask = gph.adjustedGateActivationMask(reusableSample);

        //I choose 2 (or later on at least 2) constraint polytopes by choosing a single gate variable
        int chosenGateVarIndex = random.nextInt(gateMask.size()/*exclusive*/);

        if (DEBUG) System.out.println("gateMask = " + gateMask + "\t chosen gate index: " + chosenGateVarIndex);

        gateMask.set(chosenGateVarIndex, true);
        ConstrainedPolynomial posKeyPolytope = gph.makePolytope(gateMask);     //todo at least one of the polytopes can be cached...
        if (DEBUG) {
            FunctionVisualizer.visualize(posKeyPolytope, -11, 11, 0.1, "pos key");
        }

        gateMask.set(chosenGateVarIndex, false);
        ConstrainedPolynomial negKeyPolytope = gph.makePolytope(gateMask);
        if (DEBUG) {
            FunctionVisualizer.visualize(negKeyPolytope, -11, 11, 0.1, "neg key");
        }

//        System.out.println("(before +- CDF) reusableVarAssign = " + Arrays.toString(reusableVarAssign) + "\t var to be sampled: " + varToBeSampled);
        final OneDimFunction posVarCDF = makeCumulativeDistributionFunction(posKeyPolytope, varToBeSampled, reusableVarAssign);
        if (DEBUG) {
            FunctionVisualizer.visualize(posVarCDF, -11, 11, 0.1, "posCDF");
        }

        final OneDimFunction negVarCDF = makeCumulativeDistributionFunction(negKeyPolytope, varToBeSampled, reusableVarAssign);
        if (DEBUG) {
            FunctionVisualizer.visualize(negVarCDF, -11, 11, 0.1, "negCDF");
        }

        OneDimFunction varCDF = new OneDimFunction() {
            @Override
            public double eval(double var) {

                double posV = posVarCDF.eval(var);
//                System.out.println("posV("+var+") = " + posV);
                double negV = negVarCDF.eval(var);
//                System.out.println("negV("+var+") = " + negV);
                return posV + negV;
            }
        };

        if (DEBUG) {
            FunctionVisualizer.visualize(varCDF, -50, 50, 0.1, "+-CDF");
        }

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

    public static double randomDoubleUniformBetween(double min, double max) {
        return random.nextDouble() * (max - min) + min;
    }

//    protected static boolean randomBoolean() {
//        return random.nextBoolean();
//    }


    Double[] cleverInitSample() {
        Double[] bestTakenSample = new Double[numVars];
        double bestSeenTarget = -1;


//        int failureCount = 0;

        Double[] sample = new Double[numVars];
        double targetValue;

        for (; ; ) {
//            if (failureCount++ > MAX_INITIAL_SAMPLING_TRIAL)
//                throw new SamplingFailureException("Unable to take initial sample");

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
}
///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

/*class PolyRejectionSampler implements SamplerInterface {

//    private VarAssignment _initialSample;
    private double m;
    private Double[] sample;

    double min;
    double max;

    GatedPolytopesHandler gph;

    public PolyRejectionSampler(GatedPolytopesHandler gph, int sampleVecSize, double minForAllVars, double maxForAllVars, double m) {
        this.gph = gph;
        this.m = m;
        sample = new Double[sampleVecSize];

        this.min = minForAllVars;
        this.max =maxForAllVars;
    }

    @Override
    public Double[] sample() throws SamplingFailureException {

        double p, u, g;
        Double[] bestSample = new Double[sample.length];

//        HashMap<String, Double> sample = new HashMap<String, Double>();
        boolean accepted = false;
        for (int attempt = 0; attempt < 1000 && !accepted; attempt++) {
            g = 1;
            for (int i=0; i<sample.length; i++) {
                sample[i] = GatedPolytopesSampler.randomDoubleUniformBetween(min, max);
                g = (max - min) * g;
            }

            g = 1.0 / g; // probability
            p = super.context.evaluate(super.rootId, null, sample);
            u = XaddSampler.randomDoubleUniformBetween(0, 1);
            if (u < (p / (m * g))) {
                accepted = true;
            }
        }

        if (!accepted)
            sample = lastSample;
        else
            _noAccepted++;

        for (String key : sample.keySet()) {
            super.reusableVarAssignment.getContinuousVarAssign().put(key, sample.get(key));
        }
    }

    private int _noAccepted = 0;
    private static int counter = 1;

    public void finish() {
        Utils.writeMat("rejection_accepted" + (counter++) + ".mat", "noAccepted", new double[]{(double) _noAccepted});
    }
}*/
