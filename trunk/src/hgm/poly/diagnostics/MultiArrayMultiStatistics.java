package hgm.poly.diagnostics;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 4/09/14
 * Time: 4:12 PM
 */
public class MultiArrayMultiStatistics {
    private Double[] meanFirstSampledErrs; //E{X}:: {E{alg_1_err1, alg2_err1, ...algN_err1}, E{alg_1_err2, alg2_err2, ...algN_err2},...}
    private double[] exp2FirstSampledErrs; //E{X^2}
    private int minFirstSamplesTakenPerIterations;
    private int numRunsPerAlgorithm;

    public MultiArrayMultiStatistics(int upperBoundOnSizeOfEntryVectors /*numMinDesiredSamplesPerRun*/,
                                     int numRunsPerAlgorithm) {
        minFirstSamplesTakenPerIterations = upperBoundOnSizeOfEntryVectors;

        meanFirstSampledErrs = new Double[upperBoundOnSizeOfEntryVectors]; //E{X}:: {E{alg_1_err1, alg2_err1, ...algN_err1}, E{alg_1_err2, alg2_err2, ...algN_err2},...}
        Arrays.fill(meanFirstSampledErrs, 0d);

        exp2FirstSampledErrs = new double[upperBoundOnSizeOfEntryVectors];

        this.numRunsPerAlgorithm = numRunsPerAlgorithm;
    }

    public void addNewValue(List<Double> errList) {
        minFirstSamplesTakenPerIterations = Math.min(minFirstSamplesTakenPerIterations, errList.size());
        for (int i = 0; i < minFirstSamplesTakenPerIterations; i++) {   //the entries more than this min... are useless, discarded eventually
            Double currentErrForFirstTakenSamples = errList.get(i);
            meanFirstSampledErrs[i] = meanFirstSampledErrs[i] + (currentErrForFirstTakenSamples / (double) numRunsPerAlgorithm);
            exp2FirstSampledErrs[i] = exp2FirstSampledErrs[i] + (currentErrForFirstTakenSamples * currentErrForFirstTakenSamples) / (double) numRunsPerAlgorithm;
        }
    }

    public List<Double> computeMean() {
        if (meanFirstSampledErrs.length != minFirstSamplesTakenPerIterations) {
            //prune the useless end of the mean array:
            Double[] resizeArray = new Double[minFirstSamplesTakenPerIterations];
            System.arraycopy(meanFirstSampledErrs, 0, resizeArray, 0, minFirstSamplesTakenPerIterations);
            meanFirstSampledErrs = resizeArray;
        }

        return Arrays.asList(meanFirstSampledErrs);
    }

    public List<Double> computeCorrectedVariance() {
        double correctedN = numRunsPerAlgorithm == 1? 1d : numRunsPerAlgorithm-1d;  //do I need correction?

        Double[] correctedVariances = new Double[minFirstSamplesTakenPerIterations];
        for (int i = 0; i < minFirstSamplesTakenPerIterations; i++) {
            correctedVariances[i] = (exp2FirstSampledErrs[i] - meanFirstSampledErrs[i] * meanFirstSampledErrs[i]) * (double) numRunsPerAlgorithm / correctedN; //note! the mult. div. is for correction.
        }

        return Arrays.asList(correctedVariances);
    }

    public List<Double> computeVariance() {
        Double[] correctedVariances = new Double[minFirstSamplesTakenPerIterations];
        for (int i = 0; i < minFirstSamplesTakenPerIterations; i++) {
            correctedVariances[i] = (exp2FirstSampledErrs[i] - meanFirstSampledErrs[i] * meanFirstSampledErrs[i]);
        }

        return Arrays.asList(correctedVariances);
    }


    public List<Double> computeCorrectedStdErr() {
        double rootNumRuns = Math.sqrt(numRunsPerAlgorithm);
        List<Double> correctedVariances = computeCorrectedVariance();
        Double[] stdErrs4FirstSamples = new Double[correctedVariances.size()];
        for (int i = 0; i < minFirstSamplesTakenPerIterations; i++) {
            stdErrs4FirstSamples[i] = Math.sqrt(correctedVariances.get(i)) / rootNumRuns;
        }
        return Arrays.asList(stdErrs4FirstSamples);
    }

    public int numberOfSamples() {
        return meanFirstSampledErrs.length;
    }

    public int numberOfChains() {
        return numRunsPerAlgorithm;
    }
}
