package hgm.poly.bayesian;

import hgm.poly.sampling.SamplerInterface;
import hgm.sampling.SamplingFailureException;

import java.util.Arrays;

/**
 * Created by Hadi Afshar.
 * Date: 11/03/14
 * Time: 9:05 PM
 * This is the sampler with EXTRA assumptions over the model that no other sampler is allowed to have, leading to better performance used to simulate the real distribution
 */
public class OriginalModelRejectionBasedGeneralBayesianSampler implements SamplerInterface {
    GeneralBayesianPosteriorHandler gph;
    double[] cVarMins;
    double[] cVarMaxes;
    int numVars;

    //Extra:
    double dynamicEnvelope;

/*
    public static OriginalModelRejectionBasedGeneralBayesianSampler makeSampler(GeneralBayesianPosteriorHandler gph, double minForAllVars, double maxForAllVars, double envelope,
                                                                                double eta) {
        int varNum = gph.getPolynomialFactory().getAllVars().length;
        double[] cVarMins = new double[varNum];
        double[] cVarMaxes = new double[varNum];
        Arrays.fill(cVarMins, minForAllVars);
        Arrays.fill(cVarMaxes, maxForAllVars);
        return new OriginalModelRejectionBasedGeneralBayesianSampler(gph, cVarMins, cVarMaxes, envelope, eta);
    }
*/

    public OriginalModelRejectionBasedGeneralBayesianSampler(GeneralBayesianPosteriorHandler gph/*, double[] cVarMins, double[] cVarMaxes*/, double envelopeCoef, double eachLikelihoodUpperBound) {
        this.gph = gph;
        this.cVarMins = gph.getPriorHandler().getLowerBoundsPerDim();//cVarMins;
        this.cVarMaxes = gph.getPriorHandler().getUpperBoundsPerDim();//cVarMaxes;
        numVars = cVarMins.length;
        if (cVarMaxes.length != numVars) throw new RuntimeException("length mismatch between mins and maxes");

        if (gph.getPolynomialFactory().getAllVars().length != numVars) throw new RuntimeException("var size mismatch");
        if (envelopeCoef <= 0d) throw new RuntimeException("envelope should be positive");
        this.dynamicEnvelope = gph.getPriorHandler().getFunctionUpperBound() * envelopeCoef * Math.pow(eachLikelihoodUpperBound, (double) gph.getLikelihoods().size());
    }

    @Override
    public Double[] reusableSample() throws SamplingFailureException {
        Double[] sample = new Double[numVars];
        for (; ; ) {
            for (int i = 0; i < numVars; i++) {
                sample[i] = AbstractGeneralBayesianGibbsSampler.randomDoubleUniformBetween(cVarMins[i], cVarMaxes[i]);
            }

            double v = gph.evaluate(sample);
            double pr = v / dynamicEnvelope;
            if (pr > 1.0000001) //todo maybe precision error???
                throw new RuntimeException("sampled value: f" + Arrays.toString(sample) + " = " + v + "\t is greater than envelope " + dynamicEnvelope);

            if (pr >= AbstractGeneralBayesianGibbsSampler.randomDoubleUniformBetween(0, 1)) {
                return sample;
            }
        }
    }
}