package hgm.sampling.gibbs;

import hgm.sampling.VarAssignment;
import xadd.XADD;

/**
 * Created by Hadi Afshar.
 * Date: 4/02/14
 * Time: 11:38 AM
 */
public class FastCollapsedGibbsSamplerWithCDFsPerSample extends GibbsSamplerWithCDFsPerSample {
    public FastCollapsedGibbsSamplerWithCDFsPerSample(XADD context, XADD.XADDNode root, VarAssignment initialSample) {
        super(context, root, initialSample);
    }
/*

    @Override
    protected void sampleSingleContinuousVar(String varToBeSampled, VarAssignment reusableVarAssign) {

//        double leafUpperBound = 1;//todo: I do not calc. upper bound since it SHOULD BE assumed that all xadd regions are attached to a MAX already...
        LazyPiecewise1DPolynomial varCDF = makeLazyCumulativeDistributionFunction(root, varToBeSampled, reusableVarAssign);


        //todo working with these maps is not recommended... Take min and max in constructor....
        Double maxVarValue = context._hmMaxVal.get(varToBeSampled);
        Double minVarValue = context._hmMinVal.get(varToBeSampled);

        //to calculate F(infinity):
//        reusableVarAssign.assignContinuousVariable(varToBeSampled, maxVarValue + 0.1 */
/*just to be exclusive*//*
); //since it is reusable
        // inverse of cdfInfinity is the normalization factor:
//        Double cdfInfinity = context.evaluate(cdfId,
//                reusableVarAssignment.getBooleanVarAssign(), reusableVarAssign.getContinuousVarAssign());
        Double cdfInfinity = varCDF.value(maxVarValue + 0.1);

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

            approxS = varCDF.value(average);

            if (approxS < s) {
                low = average;
            } else {
                high = average;
            }
        } while ((Math.abs(approxS - s) > SAMPLE_ACCURACY) && counter++ < MAX_ITERATIONS_TO_APPROX_F_INVERSE);

        // here the sample is stored....
        reusableVarAssign.assignContinuousVariable(varToBeSampled, average);
    }

    public Piecewise1DPolynomial makeLazyCumulativeDistributionFunction(XADD.XADDNode func, String integrationVar, VarAssignment currentVarAssign, double leafUpperBound) {


//        Piecewise1DPolynomial cdf = integrator.integrate(func, var, currentVarAssign.getContinuousVarAssign());//context.getExistNode(instantiatedXaddNodId));

        /*/
/****************************************
        //1. Exclude the integration var from the assignment and replace doubles with expressions
        HashMap<String, Double> continuousVarAssign = currentVarAssign.getContinuousVarAssign();
        HashMap<String, ExprLib.ArithExpr> substitution = new HashMap<String, ExprLib.ArithExpr>(Math.max(0, continuousVarAssign.size() - 1)); //since the int. var. is not added to it
        for (Map.Entry<String, Double> cVarValue : continuousVarAssign.entrySet()) {
            String cVar = cVarValue.getKey();
            if (!cVar.equals(integrationVar)) { //var should be excluded!
                Double cAssign = cVarValue.getValue();
                substitution.put(cVar, new ExprLib.DoubleExpr(cAssign));
            }
        }

        //2.
        List<PolynomialInAnInterval> polynomials = integrator.substituteAndConvertToPiecewisePolynomial(func, substitution, new Interval(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));

        //todo....................
//        return LazyCumulativeDistributionFunction lazyCDF = new LazyCumulativeDistributionFunction(polynomials, integrationVar, leafUpperBound, integrator);
             return null;
    }

    class LazyCumulativeDistributionFunction extends Piecewise1DPolynomial {

        public LazyCumulativeDistributionFunction(List<PolynomialInAnInterval> polynomials, String integrationVar, double leafUpperBound, OneDimIntegral integrator) {
            super(integrationVar);
            //
//            Piecewise1DPolynomial cdf = integrator.integrate(polynomials, integrationVar);
//            return cdf;
        }
    }

*/
}