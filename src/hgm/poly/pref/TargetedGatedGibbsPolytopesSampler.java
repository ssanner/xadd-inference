package hgm.poly.pref;

import hgm.poly.ConstrainedPolynomial;
import hgm.poly.integral.OneDimFunction;
import hgm.poly.integral.PiecewiseOffset1DPolynomial;
import hgm.sampling.gibbs.integral.Interval;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 11/03/14
 * Time: 9:05 PM
 * <p/>
 * This version chooses gating variables
 */
public class TargetedGatedGibbsPolytopesSampler extends AbstractPolytopesSampler {
    static final double minuscule = BayesianPairwisePreferenceLearningModel.C / (double) 1000000;

    public static TargetedGatedGibbsPolytopesSampler makeCleverGibbsSampler(ConstantBayesianPosteriorHandler gph, double minForAllVars, double maxForAllVars, Double[] reusableInitialSample) {
        int varNum = gph.getPolynomialFactory().getAllVars().length;
        double[] cVarMins = new double[varNum];
        double[] cVarMaxes = new double[varNum];
        Arrays.fill(cVarMins, minForAllVars);
        Arrays.fill(cVarMaxes, maxForAllVars);
        return new TargetedGatedGibbsPolytopesSampler(gph, cVarMins, cVarMaxes, reusableInitialSample);
    }

    public TargetedGatedGibbsPolytopesSampler(ConstantBayesianPosteriorHandler gph, double[] cVarMins, double[] cVarMaxes, Double[] reusableInitialSample) {
        super(gph, cVarMins, cVarMaxes, reusableInitialSample);
    }

    @Override
    protected void sampleSingleContinuousVar(String varToBeSampled, int varIndexToBeSampled, Double[] reusableVarAssign) throws FatalSamplingException { //todo: only var-index should be enough
        //todo: important: it is wrong to sample from all variables in the factory.... One should only sample from the variables in the cp


        List<Boolean> gateMask = gph.adjustedReusableGateActivationMask(reusableVarAssign);
        ConstrainedPolynomial currentKeyPolytope = gph.makePolytope(gateMask);
        final PiecewiseOffset1DPolynomial currentCDF;
        try {
            currentCDF = (PiecewiseOffset1DPolynomial) //since current partition cannot be ZERO, the casting should cause no problem...
                    makeCumulativeDistributionFunction(currentKeyPolytope, varToBeSampled, reusableVarAssign);
        } catch (ClassCastException e) {
            throw new FatalSamplingException("CDF of the current region is PiecewiseOffset1DPolynomial.ZERO_1D_FUNCTION however " +
                    "this should not happen since at least the current region has positive mass... " +
                    "(happens in sharp edges such as [0,0..,0]? where just s single node has mass which is not captured by integral...?)" + e.toString());
        }
        List<Interval> currentIntervals = currentCDF.getIntervals();
        Interval currInterval;
        if (currentIntervals.size() == 1) {
            currInterval = currentIntervals.get(0);
        } else {
            currInterval = currentIntervals.get(random.nextInt(currentIntervals.size()));
        }

        boolean leftRatherThanRight = random.nextBoolean();
        double aPointInAdjRegion;
        if (leftRatherThanRight) {
//            System.out.println("currInterval.getLowBound() = " + currInterval.getLowBound());
            aPointInAdjRegion = currInterval.getLowerBound() - minuscule;
//            System.out.println("aPointInAdjRegion = " + aPointInAdjRegion);
            if (DEBUG && aPointInAdjRegion == currInterval.getLowerBound()) throw new RuntimeException();
        } else {
            aPointInAdjRegion = currInterval.getUpperBound() + minuscule;
            if (DEBUG && aPointInAdjRegion == currInterval.getUpperBound()) throw new RuntimeException();
        }

        reusableVarAssign[varIndexToBeSampled] = aPointInAdjRegion;
        gateMask = gph.adjustedReusableGateActivationMask(reusableVarAssign);
        //todo maybe a debug is necessary to check that the updated gate mask only differs with itself in one bit...
        ConstrainedPolynomial adjKeyPolytope = gph.makePolytope(gateMask);  //adjacent region to left or right...
        final OneDimFunction adjCDF = makeCumulativeDistributionFunction(adjKeyPolytope, varToBeSampled, reusableVarAssign);

        if (DEBUG) {
//            FunctionVisualizer.visualize(currentKeyPolytope, -11, 11, 0.1, "current key");
//            FunctionVisualizer.visualize(adjKeyPolytope, -11, 11, 0.1, "adj. key");
//            FunctionVisualizer.visualize(currentCDF, -11, 11, 0.1, "curr. CDF");
//            FunctionVisualizer.visualize(adjCDF, -11, 11, 0.1, "adj. CDF");
        }


        OneDimFunction varCDF = new OneDimFunction() {
            @Override
            public double eval(double var) {

                double e1 = currentCDF.eval(var);
                double e2 = adjCDF.eval(var);
                return e1 + e2;
            }
        };

        if (DEBUG) {
//            FunctionVisualizer.visualize(varCDF, -50, 50, 0.1, "+-CDF");
        }

        double maxVarValue = cVarMaxes[varIndexToBeSampled];
        double minVarValue = cVarMins[varIndexToBeSampled];

        double s = takeSampleFrom1DFunc(varCDF, minVarValue, maxVarValue);

        // here the sample is stored....
        reusableVarAssign[varIndexToBeSampled] = s;

    }
}