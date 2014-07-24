package hgm.poly.bayesian;

import java.util.Random;

/**
 * Created by Hadi Afshar.
 * Date: 29/05/14
 * Time: 3:34 AM
 */
public class SelfTunedMetropolisHastingGeneralBayesianSampler extends MetropolisHastingGeneralBayesianSampler {
//    Random random = new Random();

    public SelfTunedMetropolisHastingGeneralBayesianSampler(GeneralBayesianPosteriorHandler gph, double proposalVarianceUpperBound, int numProposalVariancesToBeTried, int numStepsInEachTrial) {
        super(gph, proposalVarianceUpperBound);
        tuneProposalVariance(proposalVarianceUpperBound, numProposalVariancesToBeTried, numStepsInEachTrial);
    }

    private void tuneProposalVariance(double proposalVarianceUpperBound, int numAssessedParams, int numStepsInEachTuning) {
        double lastObtainedAverageAcceptanceRatio = 3; //just something more than 1, to be replaced...
        Double chosenPVar = null;

        double pVarIncrease = proposalVarianceUpperBound / (double) numAssessedParams;

        for (int i = 1; i <= numAssessedParams; i++) {
            double pVarToBeTested = pVarIncrease * i;
            super.setProposalVariance(pVarToBeTested);
            double averageAcceptanceRatio = 0d;
            for (int step = 0; step < numStepsInEachTuning; step++) {
                averageAcceptanceRatio += acceptanceRatioOfANewSample(pVarToBeTested);
            }
            averageAcceptanceRatio /= (double) numStepsInEachTuning;

            if (distanceFromMagicValue(averageAcceptanceRatio) < distanceFromMagicValue(lastObtainedAverageAcceptanceRatio)) {
                chosenPVar = pVarToBeTested;
            }
        }

        super.setProposalVariance(chosenPVar);
    }

    private double acceptanceRatioOfANewSample(double  proposalVariance) {
        Double[] proposalState = jump(lastSample, proposalVariance);

        double prCurrentState = gph.evaluate(lastSample);
        double prProposalState = gph.evaluate(proposalState);
        double acceptanceRatio = Math.min(1.0, prProposalState / prCurrentState);

        //before returning it, update the state:
        if (acceptanceRatio >= 1) {
            System.arraycopy(proposalState, 0, lastSample, 0, proposalState.length);
        }

        if (acceptanceRatio > AbstractGeneralBayesianGibbsSampler.randomDoubleUniformBetween(0, 1)) {
            System.arraycopy(proposalState, 0, lastSample, 0, proposalState.length);
        } //otherwise do nothing

        return acceptanceRatio;
    }

    private double distanceFromMagicValue(double rateOfAcceptance) {
        return Math.abs(rateOfAcceptance - 0.239); //magic number!
    }
}
