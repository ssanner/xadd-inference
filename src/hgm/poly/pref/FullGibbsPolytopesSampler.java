package hgm.poly.pref;

import hgm.poly.ConstrainedPolynomial;
import hgm.poly.integral.OneDimFunction;
import hgm.poly.vis.FunctionVisualizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 11/03/14
 * Time: 9:05 PM
 */
public class FullGibbsPolytopesSampler extends AbstractPolytopesSampler {

    public static FullGibbsPolytopesSampler makeFullGibbsSampler(ConstantBayesianPosteriorHandler gph, double minForAllVars, double maxForAllVars, Double[] reusableInitialSample) {
        int varNum = gph.getPolynomialFactory().getAllVars().length;
        double[] cVarMins = new double[varNum];
        double[] cVarMaxes = new double[varNum];
        Arrays.fill(cVarMins, minForAllVars);
        Arrays.fill(cVarMaxes, maxForAllVars);
        return new FullGibbsPolytopesSampler(gph, cVarMins, cVarMaxes, reusableInitialSample);
    }

    public FullGibbsPolytopesSampler(ConstantBayesianPosteriorHandler gph, double[] cVarMins, double[] cVarMaxes, Double[] reusableInitialSample) {
        super(gph, cVarMins, cVarMaxes, reusableInitialSample);
    }

    @Override
    protected void sampleSingleContinuousVar(String varToBeSampled, int varIndexToBeSampled, Double[] reusableVarAssign) throws FatalSamplingException { //todo: only var-index should be enough

        //todo: important: it is wrong to sample from all variables in the factory.... One should only sample from the variables in the cp
//        List<Boolean> gateMask = gph.adjustedReusableGateActivationMask(reusableVarAssign);
//        if (gateMask.size()!= gph.numberOfConstraints()) throw new RuntimeException();
        int n = gph.numberOfConstraints();
        List<Boolean> gateMask = new ArrayList<Boolean>(n);
        for (int i=0; i<n; i++) {
            gateMask.add(null);
        }

        int two2n = (int)Math.pow(2, n);
        final OneDimFunction[] polyCDFs = new OneDimFunction[two2n];

//        System.out.println("n = " + n);
//        System.out.println("two2n = " + two2n);
        for (int i = 0; i < two2n; i++) {
            int ii = i;
            for (int j=0; j<n; j++){
                gateMask.set(j, (ii%2 != 0));
                ii >>= 1;
            }
//            System.out.println("gateMask = " + gateMask);
            ConstrainedPolynomial polytope = gph.makePolytope(gateMask);
            polyCDFs[i] = makeCumulativeDistributionFunction(polytope, varToBeSampled, reusableVarAssign);
        }

        //at least one polytope CDF should be non-ZERO otherwise something has gone wrong....
        boolean aNonZeroPolytopeFuncExists = false;
        for (OneDimFunction polyCDF : polyCDFs) {
            if (!polyCDF.equals(OneDimFunction.ZERO_1D_FUNCTION)) {
                aNonZeroPolytopeFuncExists = true;
                break;
            }
        }
        if (!aNonZeroPolytopeFuncExists) throw new FatalSamplingException("all regions are zero");


        OneDimFunction varCDF = new OneDimFunction() {
            @Override
            public double eval(double var) {

                double result = 0d;
                for (OneDimFunction polyCDF : polyCDFs) {
                    result += polyCDF.eval(var);
                }

              return result;
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