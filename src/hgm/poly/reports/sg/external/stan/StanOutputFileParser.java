package hgm.poly.reports.sg.external.stan;

import java.io.*;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by Hadi Afshar.
 * Date: 13/01/15
 * Time: 4:01 PM
 */
public class StanOutputFileParser {
    public static final String END = System.lineSeparator();
    public static final int SAMPLE_HEADER = 6;

    private File outputFile; // Stan CSV output file
    String[] varsInFile;
    int numSamples;
    double totalSamplingTime;

    public StanOutputFileParser(File outputFile) throws IOException {
        this.outputFile = outputFile;

        varsInFile = fetchVariablesSampledFromInFile(outputFile);
        numSamples = fetchNumTotalSamples(outputFile);
        totalSamplingTime = fetchTotalSamplingTimeSeconds(outputFile);
    }

    public StanOutputFileParser(String outputFile) throws IOException {
        this(new File(outputFile));
    }

    public String[] getSampledVars() {
        return varsInFile;
    }

    public int getNumSamples() {
        return numSamples;
    }

    public double getTotalSamplingTime() {
        return totalSamplingTime;
    }

    public long getStanClaimedSamplingTimePerSamplePerNanoSeconds() {
        double k = 1000000000d / (double) numSamples;
        return (long)(totalSamplingTime * k);
    }

    public Iterator<Double[]> lowLevelStanSampleIterator() throws IOException {
//        double totalSamplingTime = fetchTotalSamplingTime(file);
//        System.out.println("totalSamplingTime = " + totalSamplingTime);
//        final Integer numSamples = fetchNumTotalSamples(file);
//        System.out.println("numTakenSamples = " + numSamples);
//        final String[] varsInFile = fetchVariablesSampledFromInFile(file);
//        System.out.println("varsInFile = " + Arrays.toString(varsInFile));
        String line;

        //reading

        final BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(outputFile)));

        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) break; //discard all lines till an empty line is seen
        }

        if (line == null) throw new StanParsingException("parsing error");

        return new Iterator<Double[]>() {
            int remainedSamples = numSamples;
            String line;

            @Override
            public boolean hasNext() {
                if (remainedSamples > 0) {
                    remainedSamples--;
                    return true;
                } else {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            }

            @Override
            public Double[] next() {
                try {
                    line = br.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                String[] split = line.trim().split(",");
                assertTrue(split.length == SAMPLE_HEADER + varsInFile.length);
//                System.out.println("split = " + Arrays.toString(split));
                Double[] sample = new Double[varsInFile.length];
                for (int i = 0; i < sample.length; i++) {
                    sample[i] = Double.valueOf(split[SAMPLE_HEADER + i]);
                }
                return sample;
            }

            @Override
            public void remove() {
                throw new RuntimeException("not implemented");
            }
        };

    }

    private String[] fetchVariablesSampledFromInFile(File file) throws IOException {
        BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file)));

        String line;
        while ((line = br.readLine()) != null) {
            if (!line.startsWith("#")) { //The first non-remarked line:
                String[] splits = line.trim().split(",");
//                System.out.println("splits = " + Arrays.toString(splits));
                assertTrue(splits[0].equals("lp__"));
                assertTrue(splits[1].equals("accept_stat__"));
                assertTrue(splits[2].equals("stepsize__"));
                assertTrue(splits[3].equals("treedepth__"));
                assertTrue(splits[4].equals("n_leapfrog__"));
                assertTrue(splits[5].equals("n_divergent__"));
                String[] qVars = Arrays.copyOfRange(splits, SAMPLE_HEADER, splits.length);
                for (int i = 0; i < qVars.length; i++) {
                    qVars[i] = qVars[i].replaceAll("\\.", "_");
                }
                return qVars;
            }
        }

        throw new StanParsingException("parsing error");
    }

    private int fetchNumTotalSamples(File file) throws IOException {
        BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file)));

        String line;
        while ((line = br.readLine()) != null) {
            if (!line.startsWith("#")) throw new StanParsingException("cannot parse " + line);
            if (line.contains("num_samples")) {
                String[] splits = line.split("=");
                assertTrue(splits.length == 2);
                splits = splits[1].split("\\(");
                assertTrue(splits.length <= 2);
                return Integer.parseInt(splits[0].trim());
            }
        }
        throw new StanParsingException("could not parse...");
    }

    private double fetchTotalSamplingTimeSeconds(File file) throws IOException {
        BufferedReader reverseBr = new BufferedReader(
                new InputStreamReader(new ReverseLineInputStream(file)));
        String line;
        while ((line = reverseBr.readLine()) != null) {
            if (!line.trim().isEmpty()) break;
        }
        if (line == null) throw new StanParsingException("unexpected NULL");
        if (!line.startsWith("#")) throw new StanParsingException("Cannot parse " + line);
        String[] lineSplits = line.substring(1).trim().split(" ");
        assertTrue(lineSplits[1].equals("seconds"));
        assertTrue(lineSplits[2].equals("(Total)"));
        return Double.valueOf(lineSplits[0]);
    }

    /*public static void stanExecutableModelBatchMaker(List<Pair<String*//*fileName*//*, String*//*code*//*>> filesAndCodes) throws FileNotFoundException {
        StringBuilder batch = new StringBuilder("");
        for (Pair<String, String> filesAndCode : filesAndCodes) {
            String completeFileNameNoExtension = CMD_STAN_HOME + STAN_EXPERIMENT_FOLDER + filesAndCode.getFirstEntry();
            String stanCode = filesAndCode.getSecondEntry();
            File execFile = new File(completeFileNameNoExtension + ".exe");
            if (execFile.exists()) {
                System.out.println(execFile + " already exists...");
            } else {
                String stanFileName = completeFileNameNoExtension + ".stan";
                System.out.println("persisting " + stanFileName + "...");
                PrintStream ps = new PrintStream(new FileOutputStream(stanFileName));
                ps.print(stanCode);
                ps.close();

                String relativeFileAddress =
                        STAN_EXPERIMENT_FOLDER + filesAndCode.getFirstEntry() + ".exe";
                System.out.println("adding to batch: " + relativeFileAddress + "...");
                batch.append("ECHO ----------").append(StanCodeGenerator.END);
                // start "" make xxx/bernoulli.exe
                batch.append("start \"\" make ").append(relativeFileAddress).append(END);
            }
        }
        System.out.println("batch = " + batch);
        PrintStream ps = new PrintStream(new FileOutputStream(CMD_STAN_HOME + BATCH));
        ps.print(batch);
        ps.close();
    }*/

    private void assertTrue(boolean p){
        if (!p) throw new RuntimeException("assertion failed");

    }
}

