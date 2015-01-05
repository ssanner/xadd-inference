package hgm.poly.reports.sg.external.Anglican;

import hgm.asve.Pair;
import hgm.poly.gm.JointToSampler;
import hgm.poly.gm.JointWrapper;
import hgm.poly.gm.RichJointWrapper;
import hgm.poly.reports.sg.external.ExternalMhSampleBank;
import hgm.poly.sampling.SamplerInterface;
import hgm.sampling.SamplingFailureException;

import java.util.Arrays;

/**
 * Created by Hadi Afshar.
 * Date: 3/12/14
 * Time: 2:48 AM
 */
public class AnglicanJointToSampler implements JointToSampler{
    int maxNumberOfTakenSamples;
    double observationNoiseParam;
    AnglicanCodeGenerator.AnglicanSamplingMethod samplingMethod;

    public AnglicanJointToSampler(int maxNumberOfTakenSamples, double observationNoiseParam, AnglicanCodeGenerator.AnglicanSamplingMethod samplingMethod) {
        this.maxNumberOfTakenSamples = maxNumberOfTakenSamples;
        this.observationNoiseParam = observationNoiseParam;
        this.samplingMethod = samplingMethod;
    }

    @Override
    public SamplerInterface makeSampler(JointWrapper jointWrapper) {
        if (!(jointWrapper instanceof RichJointWrapper)) throw new RuntimeException();
        final RichJointWrapper richJW = (RichJointWrapper)jointWrapper;
        String anglicanCode = richJW.extraInfo(AnglicanCodeGenerator.ANGLICAN_CODE_KEY);
        anglicanCode = anglicanCode.replaceAll(AnglicanCodeGenerator.ANGLICAN_NOISE_PARAM, Double.toString(observationNoiseParam));
        final ExternalMhSampleBank sampleBank;
        try {
            System.err.println("Anglican code running ...");
            sampleBank = AnglicanCodeGenerator.runAnglicanCode(AnglicanCodeGenerator.ANGLICAN_DEFAULT_JAR_PATH, anglicanCode, maxNumberOfTakenSamples, samplingMethod);   //pgibbs, smc, rdb
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final Double[] reusableFullFactoryLikeSample = new Double[richJW.getJoint().getFactory().getAllVars().length];

        return new SamplerInterface() {
            long numTakenSamples = 0; //just for debugging
            @Override
            public Double[] reusableSample() throws SamplingFailureException {
                if (sampleBank.hasNext()) {
                    Pair<Double[],Long> sampleAndTime = sampleBank.next();
                    long delay = System.nanoTime() + sampleAndTime.getSecondEntry();

                    while (System.nanoTime() <= delay){
                    //just wait
                    }
                    numTakenSamples++;
                    Double[] reusableAdaptedSample = reusableMimicFactorySample(sampleAndTime.getFirstEntry());
                    return reusableAdaptedSample;

                } else throw new RuntimeException("taken samples: " + numTakenSamples + " exceeds the bank capacity: " + maxNumberOfTakenSamples);
            }

            private Double[] reusableMimicFactorySample(Double[] queryValuation) {
                Arrays.fill(reusableFullFactoryLikeSample, null);
                int[] queryVarIndexes = richJW.getQueryVarIndexes();
                for (int i=0; i<queryValuation.length; i++){
                    reusableFullFactoryLikeSample[queryVarIndexes[i]] = queryValuation[i];
                }
                return reusableFullFactoryLikeSample;
            }
        };
    }

    @Override
    public String getName() {
        return "anglican." + samplingMethod.toString();
    }
}
