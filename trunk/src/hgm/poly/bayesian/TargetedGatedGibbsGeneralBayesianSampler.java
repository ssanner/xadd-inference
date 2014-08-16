package hgm.poly.bayesian;

import hgm.poly.PiecewiseExpression;
import hgm.poly.integral.OneDimFunction;
import hgm.poly.integral.PiecewiseOffset1DPolynomial;
import hgm.poly.pref.FatalSamplingException;
import hgm.sampling.gibbs.integral.Interval;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 11/03/14
 * Time: 9:05 PM
 */
public class TargetedGatedGibbsGeneralBayesianSampler extends AbstractGeneralBayesianGibbsSampler {
    double minuscule;
    private int numCasesInPrior;

    public TargetedGatedGibbsGeneralBayesianSampler(GeneralBayesianPosteriorHandler gph, Double[] reusableInitialSample) {
        super(gph, reusableInitialSample);
        numCasesInPrior = gph.getPriorHandler().getPrior().getCases().size();
        minuscule =
                (gph.getPriorHandler().getUpperBoundsPerDim()[0] -
                gph.getPriorHandler().getLowerBoundsPerDim()[0]) /10000000d; //just a small number
    }



    @Override
    protected void sampleSingleContinuousVar(int varIndexToBeSampled,
                                             Double[] reusableVarAssign) throws FatalSamplingException {

        //todo: important: it is wrong to sample from all variables in the factory.... One should only sample from the variables in the cp
        List<Integer> gateMask = gph.adjustedReusableGateActivationMask(reusableVarAssign);
        final List<OneDimFunction> partitionCDFs = new ArrayList<OneDimFunction>(10 * numCasesInPrior);

        PiecewiseExpression currentPartition = gph.makeActivatedSubFunction(gateMask);
        makeAndAddCumulativeDistributionFunctionsToList(currentPartition, varIndexToBeSampled, reusableVarAssign, partitionCDFs);
        if (partitionCDFs.isEmpty()) throw new FatalSamplingException("since current partition cannot be ZERO, the list should not be empty");
        List<Interval> currentIntervals = new ArrayList<Interval>();
        for (OneDimFunction partitionCDF : partitionCDFs) {
            currentIntervals.addAll(((PiecewiseOffset1DPolynomial) //since current partition cannot be ZERO, the list should not be empty and the casting should cause no problem...
                    partitionCDF).getIntervals());
            //find min and max of intervals todo in case, constraints are not linear, [the above] may need modifications (to avoid holes being skipped)....
        }
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Interval interval : currentIntervals) {
            min = Math.min(min, interval.getLowerBound());
            max = Math.max(max, interval.getUpperBound());
        }

        boolean leftRatherThanRight = random.nextBoolean();
        double aPointInAdjRegion;
        if (leftRatherThanRight) {
            aPointInAdjRegion = min - minuscule;
            if (DEBUG && aPointInAdjRegion == min) throw new RuntimeException();
        } else {
            aPointInAdjRegion = max + minuscule;
            if (DEBUG && aPointInAdjRegion == max) throw new RuntimeException();
        }
        reusableVarAssign[varIndexToBeSampled] = aPointInAdjRegion;
        gateMask = gph.adjustedReusableGateActivationMask(reusableVarAssign);
        PiecewiseExpression adjKeyPartitions = gph.makeActivatedSubFunction(gateMask);  //adjacent region to left or right...
        makeAndAddCumulativeDistributionFunctionsToList(adjKeyPartitions, varIndexToBeSampled, reusableVarAssign, partitionCDFs);

//        Integer numCasesInTheCollapsedLikelihood = gph.getNumCasesInLikelihood(chosenGateIndex);
//        final List<OneDimFunction> partitionCDFs = new ArrayList<OneDimFunction>(numCasesInTheCollapsedLikelihood * numCasesInPrior);
//        for (int i = 0; i < numCasesInTheCollapsedLikelihood; i++) {
//            gateMask.set(chosenGateIndex, i);
//            PiecewisePolynomial partition = gph.makeActivatedSubFunction(gateMask);
//            makeAndAddCumulativeDistributionFunctionsToList(partition, varToBeSampled, reusableVarAssign, partitionCDFs);
//        }

        if (partitionCDFs.isEmpty()) throw new FatalSamplingException("all regions are zero");

        OneDimFunction varCDF = new OneDimFunction() {
            @Override
            public double eval(double var) {
                double val = 0;
                for (OneDimFunction cdf : partitionCDFs) {
                    val += cdf.eval(var);
                }

                return val;
            }
        };

        double maxVarValue = cVarMaxes[varIndexToBeSampled];
        double minVarValue = cVarMins[varIndexToBeSampled];

        double s = takeSampleFrom1DFunc(varCDF, minVarValue, maxVarValue);

        // here the sample is stored....
        reusableVarAssign[varIndexToBeSampled] = s;
    }
}