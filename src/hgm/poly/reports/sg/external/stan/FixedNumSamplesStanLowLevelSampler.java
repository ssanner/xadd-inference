package hgm.poly.reports.sg.external.stan;

import hgm.sampling.SamplingFailureException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

/**
 * Created by Hadi Afshar.
 * Date: 18/01/15
 * Time: 1:05 AM
 */
public class FixedNumSamplesStanLowLevelSampler implements LowLevelStanSamplerInterface {
    public static boolean VERBOSE = false;

    private String[] stanOutputSampleVars;
    long numTakenSamples = 0; //just for debugging
    Iterator<Double[]> lowLevelIterator;
    long perSampleDelayNS;

    public FixedNumSamplesStanLowLevelSampler(String cmdStanHome,
                                              String cmdStanExperimentFolder,
                                              String stanModelFileName,
                                              String stanInputFileName,
                                              String stanOutputFileName) {

        try {
            long t1 = System.nanoTime();
            String outputCompleteFileName = runStanModelOnInput(cmdStanHome, cmdStanExperimentFolder, stanModelFileName,
                    stanInputFileName, stanOutputFileName);
            long t2 = System.nanoTime();

            long actualTotalSamplingTimeNS = t2 - t1;

//            System.out.println("stopNS - startNS = " + ((double) totalSamplingTime / ((double) 1000000000)));

            StanOutputFileParser outputParser = new StanOutputFileParser(outputCompleteFileName);
            long stanClaimedPerSampleDelayNS = outputParser.getStanClaimedSamplingTimePerSamplePerNanoSeconds();

            stanOutputSampleVars = outputParser.getSampledVars(); //as they appear in the sample file not as they are in the original theory
//            String[] dataVars = outputParser.getSampledVars(); //as they appear in the sample file not as they are in the original theory

            int numSamples = outputParser.getNumSamples();
            if (numSamples != 1000) {
                throw new RuntimeException(numSamples + " samples taken (rather than default 1000!");
            }
            perSampleDelayNS = actualTotalSamplingTimeNS / numSamples;

            lowLevelIterator = outputParser.lowLevelStanSampleIterator();

        } catch (Exception e) {
            throw new StanParsingException(e);

        }
    }

    @Override
    public String[] getStanOutputSampleVars() {
        return stanOutputSampleVars;
    }

    // ooo.exe sample data file=ooo.data.R
    //NOTE: stan model is the executable file, stan input is the parameter setting and stan output contains samples

    /**
     * @param stanModelFileName model input (no folder;  just file name)
     * @return model output file complete address
     * @throws IOException
     * @throws InterruptedException
     */
    private String runStanModelOnInput(String cmdStanHome, String cmdStanExperimentFolder,
                                       String stanModelFileName, String stanInputFileName, String stanOutputFileName) throws IOException, InterruptedException {
        String currentPath = cmdStanHome + cmdStanExperimentFolder;
        String command = currentPath + stanModelFileName + ".exe sample data file=" + stanInputFileName +
                " output file=" + stanOutputFileName;
        verbose("[STAN] Running command = " + command + "\n from path: " + currentPath);
        final Process p = Runtime.getRuntime().exec(
                command,
                new String[]{}, new File(currentPath));

//        final ExternalMhSampleBank bank = new ExternalMhSampleBank();
        new Thread(new Runnable() {

            @Override
            public void run() {
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                String line;

                try {
                    while ((line = input.readLine()) != null) {
                        verbose("STAN Err:\t" + line);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        new Thread(new Runnable() {

            @Override
            public void run() {
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;

                try {
                    while ((line = input.readLine()) != null) {
                        verbose("STAN    :\t" + line);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();


        p.waitFor();

        return currentPath + stanOutputFileName;
    }

    private void verbose(String s) {
        if (VERBOSE) {
            System.out.println(s);
        }
    }


    @Override
    public Double[] reusableSample() throws SamplingFailureException {
        if (lowLevelIterator.hasNext()) {
            Double[] lowLevelSample = lowLevelIterator.next();
            long delay = System.nanoTime() + perSampleDelayNS;

            while (System.nanoTime() <= delay) {
                //just wait
            }
            numTakenSamples++;
//                    Double[] reusableAdaptedSample = reusableMimicFactorySample(lowLevelSample);
//                    return reusableAdaptedSample;
            return lowLevelSample;

        } else
            throw new StanFixedSamplesExceededException("After taking " + numTakenSamples + " samples, the sample bank emptied");
    }

}