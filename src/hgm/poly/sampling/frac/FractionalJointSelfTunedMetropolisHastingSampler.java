package hgm.poly.sampling.frac;

import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;
import hgm.poly.bayesian.AbstractGeneralBayesianGibbsSampler;
import hgm.poly.gm.JointToSampler;
import hgm.poly.sampling.SamplerInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 19/08/14
 * Time: 3:07 AM
 */
public class FractionalJointSelfTunedMetropolisHastingSampler extends FractionalJointMetropolisHastingSampler {
    public static JointToSampler makeJointToSampler(final double proposalVarianceUpperBound,
                                                    final int numProposalVariancesToBeTried,
                                                    final int numStepsInEachTrial){
        return new JointToSampler() {
            @Override
            public SamplerInterface makeSampler(PiecewiseExpression<Fraction> joint, double minLimitForAllVars, double maxLimitForAllVars) {
                return FractionalJointSelfTunedMetropolisHastingSampler.makeSampler(joint, minLimitForAllVars, maxLimitForAllVars,
                        proposalVarianceUpperBound, numProposalVariancesToBeTried, numStepsInEachTrial);
            }

            @Override
            public String getName() {
                return "tuned.mh";
            }
        };
    }

    public static FractionalJointSelfTunedMetropolisHastingSampler makeSampler(PiecewiseExpression<Fraction> joint,
                                                                      double minForAllVars, double maxForAllVars,
                                                                      double proposalVarianceUpperBound,
                                                                      int numProposalVariancesToBeTried,
                                                                      int numStepsInEachTrial) {
        List<String> jointScopeVars = new ArrayList<String>(joint.getScopeVars());
        int numScopeVars = jointScopeVars.size(); // note: these are not all vars in the factory.
        double[] cVarMins = new double[numScopeVars];
        double[] cVarMaxes = new double[numScopeVars];
        Arrays.fill(cVarMins, minForAllVars);
        Arrays.fill(cVarMaxes, maxForAllVars);
        return new FractionalJointSelfTunedMetropolisHastingSampler(joint, jointScopeVars, cVarMins, cVarMaxes,
                proposalVarianceUpperBound, numProposalVariancesToBeTried, numStepsInEachTrial);
    }

    //............................

    public FractionalJointSelfTunedMetropolisHastingSampler(PiecewiseExpression<Fraction> joint,
                                                            List<String> scopeVars,
                                                            double[] cVarMins, double[] cVarMaxes,
                                                            double proposalVarianceUpperBound,
                                                            int numProposalVariancesToBeTried,
                                                            int numStepsInEachTrial) {
        super(joint, scopeVars, cVarMins, cVarMaxes, proposalVarianceUpperBound);
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
        Double[] proposalState = jump(lastSample, scopeVarIndexes, proposalVariance);

        double prCurrentState = joint.evaluate(lastSample);
        double prProposalState = joint.evaluate(proposalState);
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
