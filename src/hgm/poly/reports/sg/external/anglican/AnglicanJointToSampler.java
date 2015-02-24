package hgm.poly.reports.sg.external.anglican;

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
        anglicanCode = anglicanCode.replaceAll(AnglicanCodeGenerator.EXTERNAL_MODEL_NOISE_PARAM, Double.toString(observationNoiseParam));

        final String finalAnglicanCode = anglicanCode;

        return new SamplerInterface() {
            boolean bankFunctions = false;
            ExternalMhSampleBank sampleBank = null;
            final Double[] reusableFullFactoryLikeSample = new Double[richJW.getJoint().getFactory().getAllVars().length];
            void fullBank(){

                try {
                    System.err.println("Anglican code running ...");
                    sampleBank = AnglicanCodeGenerator.runAnglicanCode(AnglicanCodeGenerator.ANGLICAN_DEFAULT_JAR_PATH, finalAnglicanCode, maxNumberOfTakenSamples, samplingMethod);   //pgibbs, smc, rdb
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            long numTakenSamples = 0; //just for debugging
            {
                fullBank();
            }
            @Override
            public Double[] reusableSample() throws SamplingFailureException {
                if (sampleBank.hasNext()) {
                    bankFunctions = true;
                    Pair<Double[],Long> sampleAndTime = sampleBank.next();
                    long delay = System.nanoTime() + sampleAndTime.getSecondEntry();

                    while (System.nanoTime() <= delay){
                    //just wait
                    }
                    numTakenSamples++;
                    Double[] reusableAdaptedSample = reusableMimicFactorySample(sampleAndTime.getFirstEntry());
                    return reusableAdaptedSample;

                } else {
                    if (!bankFunctions) throw new SamplingFailureException("taken samples: " + numTakenSamples + " exceeds the bank capacity: " + maxNumberOfTakenSamples);
                    else bankFunctions = false;

                    System.err.println("taken samples: " + numTakenSamples + " exceeds the bank capacity: " + maxNumberOfTakenSamples);
                    fullBank();
                    return reusableSample();
                }
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
