package hgm.poly.bayesian;

import hgm.poly.sampling.SamplerInterface;
import hgm.sampling.SamplingFailureException;

import java.util.Arrays;

/**
 * Created by Hadi Afshar.
 * Date: 11/03/14
 * Time: 9:05 PM
 */
public class RejectionBasedGeneralBayesianSampler implements SamplerInterface {
    GeneralBayesianPosteriorHandler gph;
    double[] cVarMins;
    double[] cVarMaxes;
    int numVars;

    double envelope;

    /*public static RejectionBasedGeneralBayesianSampler makeSampler(GeneralBayesianPosteriorHandler gph, double minForAllVars, double maxForAllVars, double envelope) {
        int varNum = gph.getPolynomialFactory().getAllVars().length;
        double[] cVarMins = new double[varNum];
        double[] cVarMaxes = new double[varNum];
        Arrays.fill(cVarMins, minForAllVars);
        Arrays.fill(cVarMaxes, maxForAllVars);
        return new RejectionBasedGeneralBayesianSampler(gph, cVarMins, cVarMaxes, envelope);
    }
*/
    public RejectionBasedGeneralBayesianSampler(GeneralBayesianPosteriorHandler gph/*, double[] cVarMins, double[] cVarMaxes*/, double envelopeCoef) {
        this.gph = gph;
        this.cVarMins = gph.getPriorHandler().getLowerBoundsPerDim();//cVarMins;
        this.cVarMaxes = gph.getPriorHandler().getUpperBoundsPerDim();//cVarMaxes;
        numVars = cVarMins.length;
        if (cVarMaxes.length != numVars) throw new RuntimeException("length mismatch between mins and maxes");

        if (gph.getPolynomialFactory().getAllVars().length != numVars) throw new RuntimeException("var size mismatch");
        if (envelopeCoef <= 0d) throw new RuntimeException("envelope should be positive");
        this.envelope = gph.getPriorHandler().getFunctionUpperBound() * envelopeCoef;
    }

    @Override
    public Double[] reusableSample() throws SamplingFailureException {
        Double[] sample = new Double[numVars];
        for (; ; ) {
            for (int i = 0; i < numVars; i++) {
                sample[i] = AbstractGeneralBayesianGibbsSampler.randomDoubleUniformBetween(cVarMins[i], cVarMaxes[i]);
            }

            double v = gph.evaluate(sample);
            double pr = v / envelope;
            if (pr > 1)
                throw new RuntimeException("sampled value: f" + Arrays.toString(sample) + " = " + v + "\t is greater than envelope " + envelope);

            if (pr >= AbstractGeneralBayesianGibbsSampler.randomDoubleUniformBetween(0, 1)) {
                return sample;
            }
        }
    }
}