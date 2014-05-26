package hgm.poly.bayesian;

import hgm.poly.PiecewisePolynomial;
import hgm.poly.integral.OneDimFunction;
import hgm.poly.pref.FatalSamplingException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 11/03/14
 * Time: 9:05 PM
 */
public class GatedGibbsGeneralBayesianSampler extends AbstractGeneralBayesianGibbsSampler {

    /*public static GatedGibbsGeneralBayesianSampler makeSampler(GeneralBayesianPosteriorHandler gph, double minForAllVars, double maxForAllVars, Double[] reusableInitialSample) {
        int varNum = gph.getPolynomialFactory().getAllVars().length;
        double[] cVarMins = new double[varNum];
        double[] cVarMaxes = new double[varNum];
        Arrays.fill(cVarMins, minForAllVars);
        Arrays.fill(cVarMaxes, maxForAllVars);
        return new GatedGibbsGeneralBayesianSampler(gph, cVarMins, cVarMaxes, reusableInitialSample);
    }*/

    private int numCasesInPrior;

    /*public GatedGibbsGeneralBayesianSampler(GeneralBayesianPosteriorHandler gph, double[] cVarMins, double[] cVarMaxes, Double[] reusableInitialSample) {
        super(gph, cVarMins, cVarMaxes, reusableInitialSample);

        numCasesInPrior = gph.getPrior().getCases().size();
    }
    */

    public GatedGibbsGeneralBayesianSampler(GeneralBayesianPosteriorHandler gph/*, double[] cVarMins, double[] cVarMaxes*/, Double[] reusableInitialSample) {
        super(gph/*, cVarMins, cVarMaxes*/, reusableInitialSample);

        numCasesInPrior = gph.getPriorHandler().getPrior().getCases().size();
    }



    @Override
    protected void sampleSingleContinuousVar(String varToBeSampled, //todo IMPORTANT: VAR INDEX IS ENOUGH, YOU DO NOT NEED A STRING
                                             int varIndexToBeSampled,
                                             Double[] reusableVarAssign) throws FatalSamplingException { //todo: only var-index should be enough

        //todo: important: it is wrong to sample from all variables in the factory.... One should only sample from the variables in the cp

        List<Integer> gateMask = gph.adjustedReusableGateActivationMask(reusableVarAssign);


        //I choose 2 (or later on at least 2) constraint polytopes by choosing a single gate variable
        int chosenGateIndex = random.nextInt(gateMask.size()/*exclusive*/); //for each gate (i.e. each likelihood) there is a gate...

        /*if (DEBUG) System.out.println("gateMask = " + gateMask + "\t chosen gate index: " + chosenGateIndex);
        if (DEBUG) {
            System.out.println("gph.evaluate(reusableVarAssign) = " + gph.evaluate(reusableVarAssign));
            PiecewisePolynomial prior = gph.getPriorHandler().getPrior();
            System.out.println("[*] prior = " + prior);
            List<PiecewisePolynomial> likelihoods = gph.getLikelihoods();
            for (int i1 = 0; i1 < likelihoods.size(); i1++) {
                PiecewisePolynomial likelihood = likelihoods.get(i1);
                System.out.println("[*] likelihood[" + i1 + "] = " + likelihood);
            }
            for (int i = 0; i < gph.getNumCasesInLikelihood(chosenGateIndex); i++) {
                gateMask.set(chosenGateIndex, i);
                PiecewisePolynomial partition = gph.makeActivatedSubFunction(gateMask);
                System.out.println("sub func["+i+"] = " + partition);
                System.out.println("_-_-_-_- partitions[" + i + "].evaluate(reusableVarAssign) = " + partition.evaluate(reusableVarAssign));
            }
        }
        */
        Integer numCasesInTheCollapsedLikelihood = gph.getNumCasesInLikelihood(chosenGateIndex);
        final List<OneDimFunction> partitionCDFs = new ArrayList<OneDimFunction>(numCasesInTheCollapsedLikelihood * numCasesInPrior);
        for (int i = 0; i < numCasesInTheCollapsedLikelihood; i++) {
            gateMask.set(chosenGateIndex, i);
            PiecewisePolynomial partition = gph.makeActivatedSubFunction(gateMask);
            makeAndAddCumulativeDistributionFunctionsToList(partition, varToBeSampled, reusableVarAssign, partitionCDFs);
        }
        if (partitionCDFs.isEmpty()) throw new FatalSamplingException("all regions are zero");

        OneDimFunction varCDF = new OneDimFunction() {
            @Override
            public double eval(double var) {
                double val = 0;
                for (OneDimFunction cdf : partitionCDFs) {
                    val += cdf.eval(var);
                }

                return val;

//                double posV = posVarCDF.eval(var);
//                double negV = negVarCDF.eval(var);
//                return posV + negV;
            }
        };

//        if (DEBUG) {
//            FunctionVisualizer.visualize(varCDF, -50, 50, 0.1, "+-CDF");
//        }

        double maxVarValue = cVarMaxes[varIndexToBeSampled];
        double minVarValue = cVarMins[varIndexToBeSampled];

        double s = takeSampleFrom1DFunc(varCDF, minVarValue, maxVarValue);

        // here the sample is stored....
        reusableVarAssign[varIndexToBeSampled] = s;
    }
}