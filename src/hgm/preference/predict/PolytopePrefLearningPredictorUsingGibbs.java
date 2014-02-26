package hgm.preference.predict;

import hgm.sampling.XaddSampler;
import hgm.sampling.VarAssignment;
import hgm.sampling.gibbs.GibbsSamplerWithCDFsPerSample;
import xadd.XADD;

/**
 * Created by Hadi Afshar.
 * Date: 3/02/14
 * Time: 4:06 PM
 */
public class PolytopePrefLearningPredictorUsingGibbs extends PolytopePrefLearningPredictor {
    public PolytopePrefLearningPredictorUsingGibbs(double indicatorNoise,
                                                   boolean reduceLP,
                                                   int numberOfSamples,
                                                   double relativeLeafValueBelowWhichRegionsAreTrimmed,
                                                   double epsilon,
                                                   int burnedSamples) {
        super(indicatorNoise, reduceLP, numberOfSamples, relativeLeafValueBelowWhichRegionsAreTrimmed, epsilon, burnedSamples);
    }

    @Override
    public XaddSampler makeNewSampler(XADD context, XADD.XADDNode posterior, VarAssignment initAssignment) {
        return new GibbsSamplerWithCDFsPerSample(context, posterior, initAssignment);
    }
}
