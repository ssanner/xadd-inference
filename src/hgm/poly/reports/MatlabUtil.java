package hgm.poly.reports;

import java.io.*;
import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 1/06/14
 * Time: 4:36 AM
 */
public class MatlabUtil {
    public static void main(String[] args) throws IOException {
        MatlabUtil inst = new MatlabUtil();
        inst.accumulateBPPLRunsForDimAnalysisTest();
//        inst.accumulateBPPLRunsForDataAnalysisTest();
//        inst.accumulateMMMRunsForDimAnalysisTest();
//        inst.accumulateMMMRunsForDataAnalysisTest();

    }

    private void accumulateMMMRunsForDataAnalysisTest() throws IOException {   //dim analysis, fixed data
        String rootPath = "E:/REPORT_PATH3/MMM-DATA/";
        String[] subFolders = (
                "1 2 3 4 5 6 7 8 9 10 11 12 " +
                "20 21 22 23 24 25 26 27 28 29 30 31 " +
                        "40 41 42 43 44 45 " +
                        "50 51 52 53 54 55 " +
                        "56 57 58 59 60 61 " +
        "70 71 72 73 74 75").split(" ");
        //{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"};

        String[] algorithms = Nips2014FastBayesTester.TESTED_ALGORITHMS_FOR_FINAL_MMM_TESTS;
        int fixedDim = Nips2014FastBayesTester.FIXED_DIM_FOR_MMM_DATA_ANALYSIS;//8;//6;//12;

        Map<String/*algorithm*/, VarToValues> algInfo = new HashMap<String, VarToValues>();
        for (String alg : algorithms) {
            algInfo.put(alg, new VarToValues());

            for (String subFolder : subFolders) {
                try {
                    Map<Integer, Double> d = readFile(rootPath + subFolder + "/fixed-dim" + fixedDim + "-" + alg);
                    algInfo.get(alg).addInfo(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        Map<String/*algorithm*/, VarToValues> meanStdErrInfo = calcMeanStdErr(algInfo);
        saveMeanStdErr(rootPath + "mean/", "dim", fixedDim, meanStdErrInfo);
        System.out.println("MMM Data analysis (fixed dim) END");
    }

    private void accumulateMMMRunsForDimAnalysisTest() throws IOException {   //dim analysis, fixed data
        String rootPath = "E:/REPORT_PATH3/MMM-DIM/";
        String[] subFolders = //{"6", "7", "8", "9", "10", "1", "2", "3", "4", "5", "100"};
                ("1 2 3 4 5 6 7 8 9 10 " +
                        "100 101 102 103 104 105 106 107 108 109 110 111 112 113 120 121 122 123 124").split(" ");
        String[] algorithms = Nips2014FastBayesTester.TESTED_ALGORITHMS_FOR_FINAL_MMM_TESTS;
        int fixedData = Nips2014FastBayesTester.FIXED_DATA_FOR_MMM_DIM_ANALYSIS;//8;//6;//12;

        Map<String/*algorithm*/, VarToValues> algInfo = new HashMap<String, VarToValues>();
        for (String alg : algorithms) {
            algInfo.put(alg, new VarToValues());

            for (String subFolder : subFolders) {
                Map<Integer, Double> d = readFile(rootPath + subFolder + "/fixed-data" + fixedData + "-" + alg);
                algInfo.get(alg).addInfo(d);
            }
        }

        Map<String/*algorithm*/, VarToValues> meanStdErrInfo = calcMeanStdErr(algInfo);
        saveMeanStdErr(rootPath + "mean/", "data", fixedData, meanStdErrInfo);
        System.out.println("MMM Dim analysis (fixed data) END");
    }

    private void accumulateBPPLRunsForDimAnalysisTest() throws IOException { //fixed data
        String rootPath = "E:/REPORT_PATH3/BPPL-DIM/";//"E:/REPORT_PATH2/";
        String[] subFolders = //{"1", "2", "3"};
                ("1 2 3 10 11 12 13 14 15 16 17 18 19 20 21").split(" ");
        String[] algorithms = Nips2014FastBayesTester.TESTED_ALGORITHMS_FOR_FINAL_BPPL_TESTS;
        int fixedData = Nips2014FastBayesTester.FIXED_DATA_FOR_BPPL_FINAL_DIM_ANALYSIS;

        Map<String/*algorithm*/, VarToValues> algInfo = new HashMap<String, VarToValues>();
        for (String alg : algorithms) {
            algInfo.put(alg, new VarToValues());

            for (String subFolder : subFolders) {
                Map<Integer, Double> d = readFile(rootPath + subFolder + "/fixed-data" + fixedData + "-" + alg);
                algInfo.get(alg).addInfo(d);
            }
        }

        Map<String/*algorithm*/, VarToValues> meanStdErrInfo = calcMeanStdErr(algInfo);
        saveMeanStdErr(rootPath + "mean/", "data", fixedData, meanStdErrInfo);
        System.out.println("END....");
    }

    private void accumulateBPPLRunsForDataAnalysisTest() throws IOException {
        String rootPath = "E:/REPORT_PATH3/BPPL-DATA/";//"E:/REPORT_PATH2/";
        String[] subFolders = //{"1", "2", "3"};
                ("1 2 3 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25").split(" ");

        String[] algorithms = Nips2014FastBayesTester.TESTED_ALGORITHMS_FOR_FINAL_BPPL_TESTS;
        int fixedDim = Nips2014FastBayesTester.FIXED_DIM_FOR_FINAL_BPPL_DATA_ANALYSIS;

        Map<String/*algorithm*/, VarToValues> algInfo = new HashMap<String, VarToValues>();
        for (String alg : algorithms) {
            algInfo.put(alg, new VarToValues());

            for (String subFolder : subFolders) {
                Map<Integer, Double> d = readFile(rootPath + subFolder + "/fixed-dim" + fixedDim + "-" + alg);
                algInfo.get(alg).addInfo(d);
            }
        }

        Map<String/*algorithm*/, VarToValues> meanStdErrInfo = calcMeanStdErr(algInfo);
        saveMeanStdErr(rootPath + "mean/", "dim", fixedDim, meanStdErrInfo);
        System.out.println("END BPPL data analysis (Fixed Dim) Test");

    }


    //////////////////////////////////////////////////////////////////////////////////////////////

    private void saveMeanStdErr(String folder, String fixedElement, int fixedElementValue, Map<String/*algorithm*/, VarToValues> info) throws FileNotFoundException {
        for (String algorithmName : info.keySet()) {
            String outputFileName = folder + "mean-fixed-" + fixedElement + fixedElementValue + "-" + algorithmName;
            PrintStream ps = new PrintStream(new FileOutputStream(outputFileName));
            VarToValues varToValues = info.get(algorithmName);
            SortedSet<Integer> sortedVars = new TreeSet<Integer>(varToValues.keySet());
            for (Integer sortedVar : sortedVars) {
                Double mean = varToValues.get(sortedVar).get(0);
                Double stdErr = varToValues.get(sortedVar).get(1);
                ps.println(sortedVar + "\t" + mean + "\t" + stdErr);
            }
            ps.close();
        }
    }

    private Map<String, VarToValues> calcMeanStdErr(Map<String, VarToValues> algInfo) {
        Map<String, VarToValues> result = new HashMap<String, VarToValues>();
        for (String alg : algInfo.keySet()) {
            VarToValues varToValues = algInfo.get(alg);
            VarToValues varToMeanStdErr = new VarToValues();
            for (Integer var : varToValues.keySet()) {
                List<Double> values = varToValues.get(var);
                Double mean = mean(values);
                Double stdErr = stdErr(values);
                varToMeanStdErr.put(var, Arrays.asList(mean, stdErr));
            }
            result.put(alg, varToMeanStdErr);
        }
        return result;
    }

    public static Double variance(List<Double> list) {
        Double mean = mean(list);
        double result = 0d;
        for (Double v : list) {
            double d = v - mean;
            result += d * d;
        }
        result /= (double) list.size();
        return result;
    }

    public static Double stdDeviation(List<Double> list) {
        double sigma2 = variance(list);
        return Math.sqrt(sigma2);
    }

    public static Double stdErr(List<Double> list) {
        return stdDeviation(list) / Math.sqrt(list.size());

    }

    public static Double mean(List<Double> list) {
        Double mean = 0d;
        for (Double v : list) {
            mean += v;
        }
        mean /= (double) list.size();
        return mean;
    }

    private Map<Integer, Double> readFile(String fileName) throws IOException {
        Map<Integer, Double> map = new HashMap<Integer, Double>();
        File file = new File(fileName);
        if (file.isFile()) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] split = line.split("\\s+");
                if (split.length != 2)
                    throw new RuntimeException("parsing error: " + line + " with size: " + split.length);
                Integer var = Integer.parseInt(split[0]);
                Long lValue = Long.parseLong(split[1]);
                Double value = lValue / 1000000000d; //to convert (ns) to (s) //todo
                if (map.put(var, value) != null) throw new RuntimeException("already exists");
            }
            bufferedReader.close();
        } else {
            System.err.println("could not find " + fileName);
        }
        return map;
    }

    class VarToValues extends HashMap<Integer, List<Double>> {
//        Map<Integer/*x*/, List<Double>/*times*/> var2values = new HashMap<Integer, List<Double>>();

        public void addInfo(Map<Integer, Double> info) {
            for (Integer var : info.keySet()) {
                List<Double> values = this.get(var);
                if (values == null) {
                    values = new ArrayList<Double>();
                    this.put(var, values);
                }
                values.add(info.get(var));
            }

        }
    }


}

/*
BPPL adjustments:
 //Testing params:
        int[] numDimsArray = new int[]{15};//{3, 4, 5, 6, 7, 8, 10, 12, 14, 16, 18, 20, 22, 25, 26, 28, 30, 35, 40, 45, 50, 60, 80};
        int[] numObservedDataPointsArray = new int[]{2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 34, 38, 42, 48, 55, 65, 70, 80, 90, 100};//{3, 4, 5, 7, 9, 12, 15, 18, 25, 35};//{5, 7, 9, 12, 14, 18, 22, 25}; //num. observed data
        Integer numSamplesFromTesterToSimulateTrueDistribution = 1000000;//550000;//for BPPL/dim10;constr10: 50000;
        int numMinDesiredSamples = 3;//10000;//10000;
        int numIterationsForEachAlgorithm = 3;//10
        int burnedSamples = 0;

        long maxWaitingTimeForTesterToSimulateTrueDistributionMillis = (int) (15*60 * 1000); //todo!!!!!!!!!!!!
        long maxWaitingTimeForTakingDesiredSamplesMillis = (10*60 * 1000);//10 * 60 * 1000;
        long minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis = maxWaitingTimeForTakingDesiredSamplesMillis;//(int) (1 * 60 * 1000);//10*60*1000; //(int) (10* 60 * 1000); //****

        Integer numTimePointsForWhichErrIsPersisted = 500;

        double goldenErrThreshold = 3d; //Another terminating condition: If reached sampling would be terminated. If you do not like it make it 0. or negative

 */
