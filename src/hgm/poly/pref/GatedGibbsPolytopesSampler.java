package hgm.poly.pref;

import hgm.poly.ConstrainedPolynomial;
import hgm.poly.integral.OneDimFunction;
import hgm.poly.vis.FunctionVisualizer;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 11/03/14
 * Time: 9:05 PM
 */
public class GatedGibbsPolytopesSampler extends AbstractPolytopesSampler {

    public static GatedGibbsPolytopesSampler makeSampler(ConstantBayesianPosteriorHandler gph, double minForAllVars, double maxForAllVars, Double[] reusableInitialSample) {
        int varNum = gph.getPolynomialFactory().getAllVars().length;
        double[] cVarMins = new double[varNum];
        double[] cVarMaxes = new double[varNum];
        Arrays.fill(cVarMins, minForAllVars);
        Arrays.fill(cVarMaxes, maxForAllVars);
        return new GatedGibbsPolytopesSampler(gph, cVarMins, cVarMaxes, reusableInitialSample);
    }

    public GatedGibbsPolytopesSampler(ConstantBayesianPosteriorHandler gph, double[] cVarMins, double[] cVarMaxes, Double[] reusableInitialSample) {
        super(gph, cVarMins, cVarMaxes, reusableInitialSample);
    }

    @Override
    protected void sampleSingleContinuousVar(String varToBeSampled, int varIndexToBeSampled, Double[] reusableVarAssign) throws FatalSamplingException { //todo: only var-index should be enough

        //todo: important: it is wrong to sample from all variables in the factory.... One should only sample from the variables in the cp

        List<Boolean> gateMask = gph.adjustedReusableGateActivationMask(reusableVarAssign);


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

        if (posVarCDF.equals(OneDimFunction.ZERO_1D_FUNCTION) && negVarCDF.equals(OneDimFunction.ZERO_1D_FUNCTION)) {
            throw new FatalSamplingException("both - and + sides are 0!!! Reusable assign: " + Arrays.toString(reusableVarAssign));
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

        double s = takeSampleFrom1DFunc(varCDF, minVarValue, maxVarValue);

        // here the sample is stored....
        reusableVarAssign[varIndexToBeSampled] = s;
    }
}