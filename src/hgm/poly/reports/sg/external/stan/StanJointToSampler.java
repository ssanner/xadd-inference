package hgm.poly.reports.sg.external.stan;

import hgm.poly.PolynomialFactory;
import hgm.poly.gm.JointToSampler;
import hgm.poly.gm.JointWrapper;
import hgm.poly.gm.RichJointWrapper;
import hgm.poly.reports.sg.external.anglican.AnglicanCodeGenerator;
import hgm.poly.sampling.SamplerInterface;
import hgm.sampling.SamplingFailureException;

import java.io.*;
import java.util.Arrays;

/**
 * Created by Hadi Afshar.
 * Date: 16/01/15
 * Time: 1:28 PM
 */
public class StanJointToSampler implements JointToSampler {
    //    public static final String BATCH = "batch.bat";
    public static String CMD_STAN_HOME = "E:/WORK/Stan/cmdstan/";
    public static String CMD_STAN_EXPERIMENT_FOLDER = "my_experiments/";
    public static String STAN_INPUT_FILE = "data.R";
    public static String STAN_OUTPUT_FILE = "out.csv";

    public static final String STAN_INPUT_CONTENT_KEY = "stan.input.key";
    public static final String STAN_MODEL_FILE_KEY = "stan.model.key";

    //stan models: (It is assumed that files with such names should exist in the right folder..)
    public static final String STAN_COLLISION_MODEL = "collision_model"; // the .exe should be made by 'make my_experiments/collision_model.exe'
    public static final String STAN_RESISTOR_MODEL = "resistor_model"; // the .exe should be made by 'make my_experiments/resistor_model.exe'
    public static final String STAN_FERMENTATION_MODEL = "fermentation_model"; // the .exe should be made by 'make my_experiments/resistor_model.exe'

    double observationNoiseParam;

    public StanJointToSampler(double observationNoiseParam) {
        this(observationNoiseParam, "");
    }

    private String fileSuffix;

    /**
     * @param observationNoiseParam noise
     * @param fileSuffix            this suffix would be attached to stan input (data valuation) and output files (NOTE: not attached to stan model)
     */
    public StanJointToSampler(double observationNoiseParam, String fileSuffix) {
        this.observationNoiseParam = observationNoiseParam;
        this.fileSuffix = fileSuffix;
    }

    //just for test/debug...
    public static SamplerInterface makeSampler(String stanModelFileName, String stanInputFileName, String stanOutputFileName) {
        try {
            final LowLevelStanSamplerInterface lowLevelSampler =
                    new FlexibleStanLowLevelSampler(
                            CMD_STAN_HOME, CMD_STAN_EXPERIMENT_FOLDER, stanModelFileName, stanInputFileName, stanOutputFileName);

            String[] dataVars = lowLevelSampler.getStanOutputSampleVars();
//            final int[] dataVarIndexes = computeIndices(dataVars, jointWrapper.getJoint().getFactory());

            final Double[] reusableFullFactoryLikeSample = new Double[dataVars.length];

            return new SamplerInterface() {
                @Override
                public Double[] reusableSample() throws SamplingFailureException {
                    return lowLevelSampler.reusableSample();
                }
            };

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //just for test/debug...
    public static SamplerInterface makeFactoryCompatibleSampler(String stanModelFileName, String stanInputFileName, String stanOutputFileName, PolynomialFactory factory) {
        try {
            final LowLevelStanSamplerInterface lowLevelSampler =
                    new FlexibleStanLowLevelSampler(
                            CMD_STAN_HOME, CMD_STAN_EXPERIMENT_FOLDER, stanModelFileName, stanInputFileName, stanOutputFileName);

            String[] dataVars = lowLevelSampler.getStanOutputSampleVars();
//            final int[] dataVarIndexes = computeIndices(dataVars, jointWrapper.getJoint().getFactory());

//            final Double[] reusableFullFactoryLikeSample = new Double[dataVars.length];

            final int[] dataVarIndexes = computeIndices(dataVars, factory);

            final Double[] reusableFullFactoryLikeSample = new Double[factory.getAllVars().length];

            return new SamplerInterface() {
                @Override
                public Double[] reusableSample() throws SamplingFailureException {
                    return reusableMimicFactorySample(lowLevelSampler.reusableSample());
                }

                private Double[] reusableMimicFactorySample(Double[] lowLevelSample) {
                    Arrays.fill(reusableFullFactoryLikeSample, null);
                    for (int i = 0; i < lowLevelSample.length; i++) {
                        reusableFullFactoryLikeSample[dataVarIndexes[i]] = lowLevelSample[i];
                    }
                    return reusableFullFactoryLikeSample;
                }
            };

//            return new SamplerInterface() {
//                @Override
//                public Double[] reusableSample() throws SamplingFailureException {
//                    return lowLevelSampler.reusableSample();
//                }
//            };

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public SamplerInterface makeSampler(JointWrapper jointWrapper) {
        try {
            if (!(jointWrapper instanceof RichJointWrapper)) throw new RuntimeException();
            final RichJointWrapper richJW = (RichJointWrapper) jointWrapper;
            String stanInputContent = richJW.extraInfo(STAN_INPUT_CONTENT_KEY);
            stanInputContent = stanInputContent.replaceAll(AnglicanCodeGenerator.EXTERNAL_MODEL_NOISE_PARAM,
                    Double.toString(observationNoiseParam));


            String stanInputSuffixedFileName = STAN_INPUT_FILE + fileSuffix;
            String stanOutputSuffixedFileName = STAN_OUTPUT_FILE + fileSuffix;
            String inputFileFullAddress = CMD_STAN_HOME + CMD_STAN_EXPERIMENT_FOLDER + stanInputSuffixedFileName;
            generateInputFile(stanInputContent, inputFileFullAddress);
            System.out.println("[STAN] Stan input file '" + inputFileFullAddress + "' being generated ...");


            final LowLevelStanSamplerInterface lowLevelSampler =
//                    new FixedNumSamplesStanLowLevelSampler(
                    new FlexibleStanLowLevelSampler(
                            CMD_STAN_HOME, CMD_STAN_EXPERIMENT_FOLDER, richJW.extraInfo(STAN_MODEL_FILE_KEY),
                            stanInputSuffixedFileName, stanOutputSuffixedFileName);

            String[] dataVars = lowLevelSampler.getStanOutputSampleVars();
            final int[] dataVarIndexes = computeIndices(dataVars, jointWrapper.getJoint().getFactory());

            final Double[] reusableFullFactoryLikeSample = new Double[richJW.getJoint().getFactory().getAllVars().length];

            return new SamplerInterface() {
                @Override
                public Double[] reusableSample() throws SamplingFailureException {
                    return reusableMimicFactorySample(lowLevelSampler.reusableSample());
                }

                private Double[] reusableMimicFactorySample(Double[] lowLevelSample) {
                    Arrays.fill(reusableFullFactoryLikeSample, null);
                    for (int i = 0; i < lowLevelSample.length; i++) {
                        reusableFullFactoryLikeSample[dataVarIndexes[i]] = lowLevelSample[i];
                    }
                    return reusableFullFactoryLikeSample;
                }
            };

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static int[] computeIndices(String[] dataVars, PolynomialFactory factory) {
        int[] indexes = new int[dataVars.length];
        for (int i = 0; i < dataVars.length; i++) {
            indexes[i] = factory.getVarIndex(dataVars[i]);
        }
        return indexes;
    }

    private void generateInputFile(String stanInputContent, String inputFileFullAddress) throws FileNotFoundException {
//        File file = new File(CMD_STAN_HOME + CMD_STAN_EXPERIMENT_FOLDER + STAN_INPUT_FILE);
        PrintStream ps = new PrintStream(new FileOutputStream(inputFileFullAddress));
        ps.print(stanInputContent);
        ps.close();
    }


    @Override
    public String getName() {
        return "stan.hmc";
    }

}
