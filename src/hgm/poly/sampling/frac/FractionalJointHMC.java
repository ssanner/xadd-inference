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
 * Time: 3:07 AM
 */
public class FractionalJointHMC implements SamplerInterface {
    public static JointToSampler makeJointToSampler(final double leapfrogEpsilon,
                                                    final int leapfrogL) {
        return new JointToSampler() {
            @Override
//            public SamplerInterface makeSampler(PiecewiseExpression<Fraction> joint, double minLimitForAllVars, double maxLimitForAllVars) {
            public SamplerInterface makeSampler(JointWrapper jwi) {
                return FractionalJointHMC.makeSampler(jwi.getJoint(), jwi.getMinLimitForAllVars(), jwi.getMaxLimitForAllVars(),
                        leapfrogEpsilon, leapfrogL);
            }

            @Override
            public String getName() {
                return "hmc";
            }
        };
    }

    public static FractionalJointHMC makeSampler(
//            PiecewiseExpression<Fraction> joint,
            FactorizedPiecewiseStructure<Fraction> joint,
                                                 double minForAllVars, double maxForAllVars,
                                                 double epsilon,
                                                 int l) {
        List<String> jointScopeVars = new ArrayList<String>(joint.getScopeVars());
        int numScopeVars = jointScopeVars.size(); // note: these are not all vars in the factory.
        double[] cVarMins = new double[numScopeVars];
        double[] cVarMaxes = new double[numScopeVars];
        Arrays.fill(cVarMins, minForAllVars);
        Arrays.fill(cVarMaxes, maxForAllVars);
        return new FractionalJointHMC(joint, jointScopeVars, cVarMins, cVarMaxes, epsilon, l);
    }

    public long numRejectionSamplesCount = 0;
    public long numTakenSamples = 0;
//    public double[] sumOfSamples;


    int numScopeVars;
//    PiecewiseExpression<Fraction> joint;
    FactorizedPiecewiseStructure<Fraction> joint;
    Map<Integer, Double> varIndex2MinMap;
    Map<Integer, Double> varIndex2MaxMap;

    Double[] reusableFactorySample; //current sample. External in the sense that all variables exist in the vector with the order they appear in the factory

    int[] scopeVarIndexes;

    PolynomialFactory factory;
    private Map<String, Double> reusableSampleAssignment;

    private double epsilon;
    private int l;
    Double[] q0; //q0 is the inner sample vector.

    double[] reusableGrads;

    public FractionalJointHMC(
//            PiecewiseExpression<Fraction> joint,
            FactorizedPiecewiseStructure<Fraction> joint,
                              List<String> scopeVars, double[] cVarMins, double[] cVarMaxes, double epsilon, int l) {
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

        this.reusableFactorySample = new Double[allVars.length];
        q0 = new Double[numScopeVars];
        takeInitialSample(reusableFactorySample, q0);//reusableInitialSample;//reusableSample();

        double initSampleValue = joint.evaluate(reusableFactorySample);
//        debug("initSampleValue (" + Arrays.toString(lastSample) + ") = " + initSampleValue);
        if (initSampleValue <= 0.0) {
            throw new SamplingFailureException("valuation of the initial sample is not positive: " + initSampleValue);
        }

        if (epsilon <= 0d) throw new RuntimeException("negative eps!!!");
        this.epsilon = epsilon;
        this.l = l;
        reusableGrads=new double[numScopeVars];

    }

    /**
     * uniformly sample each variable in the interval between its min and max values and reject the produced sample if its probability is not positive...
     */
    protected void takeInitialSample(Double[] reusableSample, Double[] innerQ) throws SamplingFailureException { //todo: maybe rejection based sampling should be used....
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

                innerQ[i] = AbstractGeneralBayesianGibbsSampler.randomDoubleUniformBetween(minVarValue, maxVarValue);
                reusableSample[scopeVarIndex] = innerQ[i];
            }

            targetValue = joint.evaluate(reusableSample);
        } while (targetValue <= 0.0); // a valid sample is found
    }


    private Double[] convertInnerSampleToFactorySample(Double[] innerQ, Double[] factorySample) {
        for (int i = 0; i < numScopeVars; i++) {
            int scopeVarIndex = scopeVarIndexes[i];

            factorySample[scopeVarIndex] = innerQ[i];
        }

        return factorySample;
    }

    @Override
    public Double[] reusableSample() throws SamplingFailureException {
        Double[] innerQ = reusableInnerSample();
        reusableFactorySample = convertInnerSampleToFactorySample(innerQ, reusableFactorySample);
        return reusableFactorySample;
    }

    private Double[] reusableInnerSample() {
        Double[] q = q0.clone();
        double p0[] = multiVarRandomNormal(q0.length);
        double[] p = p0.clone();

        if (atLeastOneInfinityInArray(q)) {
            throw new RuntimeException("q infinity" + Arrays.toString(q));
        }

        if (atLeastOneNullInArray(q)) {
            throw new RuntimeException("q null" + Arrays.toString(q));
        }

        debug("\np1 = " + Arrays.toString(p) + "\t" + "q1 = " + Arrays.toString(q));
        /*
         Make a half step for momentum at the beginning:
        */
        /*p = */
        momentumStep(p, q, epsilon * 0.5, joint);

        //alternate full steps for position and momentum

        for (int i = 1; i <= l; i++) {

            debug("p2 = " + Arrays.toString(p) + "\tq2 = " + Arrays.toString(q) + "\t after first momentum step");

            // Make a full step for position:
            /*p&q = */
            positionStep(p, q, epsilon);

            debug("p3 = " + Arrays.toString(p) + "\tq3 = " + Arrays.toString(q) + "\t after position step **");

            // Make a full step for the momentum except at the end of trajectory:
            if (i != l) {
                momentumStep(p, q, epsilon, joint);

                debug("p4 = " + Arrays.toString(p) + "\t" + "q4 = " + Arrays.toString(q) + "\t after second momentum step");

//                logger.logSample(q, SampleLogger.SampleType.INTERMEDIATE);

            }
        }

        // Make a half step for momentum at the end:
        momentumStep(p, q, epsilon * 0.5, joint);

        debug("p5 = " + Arrays.toString(p) + "\t" + "q5 = " + Arrays.toString(q));

        double currentH = calcHamiltonian(p0, q0, joint);
        double proposedH = calcHamiltonian(p, q, joint);
        double dH = proposedH - currentH; //currentU - proposedU + currentK - proposedK;

        if (AbstractGeneralBayesianGibbsSampler.randomDoubleUniformBetween(0, 1) < Math.exp(-dH)) {
            q0 = q; //accept
//            logger.logSample(q0, SampleLogger.SampleType.ACCEPTED);

            debug("dH = " + dH + " \t[accepted]");

        } else {
            //reject
            numRejectionSamplesCount++;
//            logger.logSample(q, SampleLogger.SampleType.REJECTED);
//            logger.logSample(q0, SampleLogger.SampleType.ACCEPTED);

            debug("dH = " + dH + " \t[rejected] \t #rejections = " + numRejectionSamplesCount);

//            if (this instanceof PieceHMC) {
//                System.out.println("rejected q = " + q + " \t>> returned: " + q0 + "\t -dH" + (-dH));
//            }
        }

        numTakenSamples++;

//        for (int i = 0; i < sumOfSamples.length; i++) {
//            sumOfSamples[i] = sumOfSamples[i] + q0[i];
//        }

        return q0;

    }

    private boolean atLeastOneInfinityInArray(Double[] ar) {
        for (Double a : ar) {
            if (Double.isInfinite(a)) return true;
        }
        return false;
    }

    private boolean atLeastOneNullInArray(Double[] ar) {
        for (Double a : ar) {
            if (a == null) return true;
        }
        return false;
    }

    private double calcHamiltonian(double p[], Double q[],
//                                   PiecewiseExpression<Fraction> expNegU /*since Pr(q) \prop EXP(-U(q))*/) {
                                   FactorizedPiecewiseStructure<Fraction> expNegU /*since Pr(q) \prop EXP(-U(q))*/) {
        double evalExpNegU = expNegU.evaluate(convertInnerSampleToFactorySample(q, reusableFactorySample));
        double _u = -Math.log(evalExpNegU);//u.evaluate(q);  //potential

        // k = sum(p^2)/2  //kinetic
        double _k = 0;
        for (int i = 0; i < p.length; i++) {
            _k += p[i] * p[i];
        }
        _k *= 0.5;

        return _u + _k;

    }

    protected void positionStep(final double[] p, final Double[] q, double time) {
        for (int i = 0; i < q.length; i++) {
            q[i] = q[i] + time * p[i];
        }
    }

    protected void momentumStep(final double p[], final Double q[], double time
//            , PiecewiseExpression<Fraction> expNegU
            , FactorizedPiecewiseStructure<Fraction> expNegU
    ) { //Full step if eps = epsilon; half step if eps = epsilon/2
        reusableGrads = evaluateAllGradsOfU(expNegU, q, reusableGrads);
        for (int i = 0; i < p.length; i++) {
            p[i] = p[i] - time * reusableGrads[i];
        }
    }


    private double[] evaluateAllGradsOfU(
//            PiecewiseExpression<Fraction> expNegU,
            FactorizedPiecewiseStructure<Fraction> expNegU,
            Double[] q, double[] reusableGrads) {
        reusableFactorySample = convertInnerSampleToFactorySample(q, reusableFactorySample);
        double negReciprocalPr = -1.0 / expNegU.evaluate(reusableFactorySample);//-1/Pr(q)
        for (int i=0; i<q.length; i++) {
            reusableGrads[i] = negReciprocalPr * gradJointWrt(scopeVarIndexes[i], reusableFactorySample, expNegU);  //-1/Pr(q) . roundPr(q)/round(q)
        }

        return reusableGrads;
    }


    private double gradJointWrt(int varIndex, Double[] reusableFactorySample,
//                                PiecewiseExpression<Fraction> joint
                                FactorizedPiecewiseStructure<Fraction> joint
    ) {

        Double currentVarValue = reusableFactorySample[varIndex];
        double delta = Math.abs(currentVarValue)/100000.0; //todo is this a right approach?

        reusableFactorySample[varIndex] = currentVarValue-delta;
        double fMinus = joint.evaluate(reusableFactorySample);

        reusableFactorySample[varIndex] = currentVarValue+delta;
        double fPlus = joint.evaluate(reusableFactorySample);

        double grad = (fPlus-fMinus)/(2.0 * delta);

        //NOTE: Ironically, here, the reusable sample should not be modified:
        reusableFactorySample[varIndex] = currentVarValue;

        return grad;
    }

    @Override
    public String toString() {
        return "BaselineHMC";
    }

    protected double[] multiVarRandomNormal(int size) {
        double[] rand = new double[size];
        for (int i = 0; i < size; i++) {
            rand[i] = AbstractGeneralBayesianGibbsSampler.randomGaussianDouble(0.0, 1.0); //with std 1
        }
        return rand;
    }

    boolean DEBUG = false;

    public void debug(String str) {
        if (DEBUG) System.out.println(str);
    }
}



