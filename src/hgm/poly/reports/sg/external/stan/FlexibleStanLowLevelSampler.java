package hgm.poly.reports.sg.external.stan;

import hgm.sampling.SamplingFailureException;

import java.util.Arrays;

/**
 * Created by Hadi Afshar.
 * Date: 18/01/15
 * Time: 2:46 AM
 * <p/>
 * No restriction on the number of taken samples
 */
public class FlexibleStanLowLevelSampler implements LowLevelStanSamplerInterface {
    public static boolean VERBOSE = false;

    private String cmdStanHome;
    private String cmdStanExperimentFolder;
    private String stanModelFileName;
    private String stanInputFileName;
    private String stanOutputFileName;
    private String[] outputVars;

    FixedNumSamplesStanLowLevelSampler innerSampler;

    public FlexibleStanLowLevelSampler(String cmdStanHome,
                                       String cmdStanExperimentFolder,
                                       String stanModelFileName,
                                       String stanInputFileName, String stanOutputFileName) {
        this.cmdStanHome = cmdStanHome;
        this.cmdStanExperimentFolder = cmdStanExperimentFolder;
        this.stanModelFileName = stanModelFileName;
        this.stanInputFileName = stanInputFileName;
        this.stanOutputFileName = stanOutputFileName;


        innerSampler = new FixedNumSamplesStanLowLevelSampler(cmdStanHome, cmdStanExperimentFolder,
                stanModelFileName, stanInputFileName, stanOutputFileName);
        outputVars = innerSampler.getStanOutputSampleVars();
    }

    @Override
    public String[] getStanOutputSampleVars() {
        return outputVars;
    }

    @Override
    public Double[] reusableSample() throws SamplingFailureException {
        try {
            return innerSampler.reusableSample();
        } catch (StanFixedSamplesExceededException e) {
            if (VERBOSE) e.printStackTrace();

            innerSampler = new FixedNumSamplesStanLowLevelSampler(cmdStanHome, cmdStanExperimentFolder,
                    stanModelFileName, stanInputFileName, stanOutputFileName);

            if (!Arrays.equals(innerSampler.getStanOutputSampleVars(), this.outputVars)) {
                throw new SamplingFailureException("var. array mismatch");
            }

            return innerSampler.reusableSample();
        }
    }
}
