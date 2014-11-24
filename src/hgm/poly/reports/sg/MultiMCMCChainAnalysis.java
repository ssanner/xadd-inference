package hgm.poly.reports.sg;

import hgm.poly.diagnostics.MultiArrayMultiStatFlexibleIndex;
import hgm.poly.diagnostics.MultiArrayMultiStatistics;
import hgm.poly.gm.JointToSampler;
import hgm.poly.gm.JointWrapper;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiMCMCChainAnalysis {
    //        int numF;
//        double[] groundTruthMeans; //this is an array of size #F not persisted but kept in case ground truth is calculated for the first time.
    MultiArrayMultiStatistics errs4FirstSamplesMultiStat;
    MultiArrayMultiStatFlexibleIndex errs4timesMultiStat;
    MultiArrayMultiStatFlexibleIndex sampleMeans4timesMultiStat;
    MultiArrayMultiStatFlexibleIndex sampleVariances4timesMultiStat;
    MultiArrayMultiStatFlexibleIndex sampleCount4timesMultiStat;
    int numRunsPerAlgorithm;   //numIterationsForEachAlgorithm
    //        MultiArrayMultiStatistics effectiveSampleSizeStat;
    ////        Long totalProcessTimeMillis;

    MultiArrayMultiStatistics timeToPassGoldenErrStat;  //Long averageTimeToAccomplishOrGolden;  //timeToTakeFirstSamplesOrGoldenTime
    MultiArrayMultiStatistics timeToTakeGoldenSamplesStat;

    /////////////////////////////////////////////////////////////////////////////////
    public MultiMCMCChainAnalysis(
            int numRunsPerAlgorithm,
            double[] groundTruthMeanVector,
            JointWrapper joint,
            JointToSampler samplerMaker,
            int burnedSamples,
            //
            final int numMinDesiredSamples,
            long maxWaitingTimeForTakingDesiredSamplesMillis, //in ms
            long minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
            int approxNumTimePointsForWhichErrIsPersisted,
            double goldenErrThreshold) {

        this.numRunsPerAlgorithm = numRunsPerAlgorithm;
        this.errs4FirstSamplesMultiStat = new MultiArrayMultiStatistics(numMinDesiredSamples, numRunsPerAlgorithm);
        this.errs4timesMultiStat = new MultiArrayMultiStatFlexibleIndex(approxNumTimePointsForWhichErrIsPersisted + 10, numRunsPerAlgorithm);
        this.sampleMeans4timesMultiStat = new MultiArrayMultiStatFlexibleIndex(approxNumTimePointsForWhichErrIsPersisted + 10, numRunsPerAlgorithm);          //for Gelman and Rubin
        this.sampleVariances4timesMultiStat = new MultiArrayMultiStatFlexibleIndex(approxNumTimePointsForWhichErrIsPersisted + 10, numRunsPerAlgorithm);      //for Gelman and Rubin
        this.sampleCount4timesMultiStat = new MultiArrayMultiStatFlexibleIndex(approxNumTimePointsForWhichErrIsPersisted + 10, numRunsPerAlgorithm);      //for Gelman and Rubin
//        MultiArrayMultiStatistics effectiveSampleSizeStat = new MultiArrayMultiStatistics(numMinDesiredSamples, numRunsPerAlgorithm);
        //List<Long> timesToAccomplishMillisOrGoldenNanos = new ArrayList<Long>(numRunsPerAlgorithm);

        timeToPassGoldenErrStat = new MultiArrayMultiStatistics(1, numRunsPerAlgorithm);
        timeToTakeGoldenSamplesStat = new MultiArrayMultiStatistics(1, numRunsPerAlgorithm);

        for (int runNum = 0; runNum < numRunsPerAlgorithm; runNum++) {
            System.out.println("\n* * *\n ALG: " + samplerMaker.getName() + " -- ITR. = " + runNum);

            SingleMCMCChainAnalysis singleMCMCChainAnalysis;

            singleMCMCChainAnalysis = new SingleMCMCChainAnalysis(groundTruthMeanVector, joint, samplerMaker, burnedSamples,
                    numMinDesiredSamples, maxWaitingTimeForTakingDesiredSamplesMillis,
                    minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis, approxNumTimePointsForWhichErrIsPersisted, goldenErrThreshold);

            //time for golden err
            if (timeToPassGoldenErrStat != null) {
                Long timeToPassGoldenErrorThresholdNanos = singleMCMCChainAnalysis.timeToPassGoldenErrorThresholdNanos;
                if (timeToPassGoldenErrorThresholdNanos == null) {
                    timeToPassGoldenErrStat = null; //I like that all chains return values otherwise I do not trust the results
                } else {
                    timeToPassGoldenErrStat.addNewValue(Arrays.asList(timeToPassGoldenErrorThresholdNanos.doubleValue()));
                }
            }

            //time for golden #samples
            if (timeToTakeGoldenSamplesStat != null) {
                Long timeToTakeGoldenNumberOfSamplesNanos = singleMCMCChainAnalysis.timeToTakeGoldenSampleCountNanos;
                if (timeToTakeGoldenNumberOfSamplesNanos == null) {
                    timeToTakeGoldenSamplesStat = null; //I like that all chains return values otherwise I do not trust the results
                } else {
                    timeToTakeGoldenSamplesStat.addNewValue(Arrays.asList(timeToTakeGoldenNumberOfSamplesNanos.doubleValue()));
                }
            }

            //means and std-errors for "errors for first samples":
            List<Double> errList = singleMCMCChainAnalysis.errForFirstTakenSamples; //err. for first taken samples
            errs4FirstSamplesMultiStat.addNewValue(errList);

            //means and std-errors for "errors in time stamps":
            List<Long> recordedTimePointsInNano = singleMCMCChainAnalysis.recordedTimePointsInNano;
            List<Double> errVsTimes = singleMCMCChainAnalysis.errVsTimes;
            errs4timesMultiStat.addNewValue(recordedTimePointsInNano, errVsTimes);

            List<Double> sampleMeanVsTimes = singleMCMCChainAnalysis.sampleMeanVsTimes;
            sampleMeans4timesMultiStat.addNewValue(recordedTimePointsInNano, sampleMeanVsTimes);

            List<Double> sampleVarianceVsTimes = singleMCMCChainAnalysis.sampleVarianceVsTimes;
            sampleVariances4timesMultiStat.addNewValue(recordedTimePointsInNano, sampleVarianceVsTimes);

            sampleCount4timesMultiStat.addNewIntegerValue(recordedTimePointsInNano, singleMCMCChainAnalysis.numTakenSamplesVsTimes);

//            effectiveSampleSizeStat.addNewValue(singleMCMCChainAnalysis.autoCorr.computeEffectiveSampleSize());


        } //end alg. run num.

/*
        if (!timesToAccomplishMillisOrGoldenNanos.isEmpty()) {
//            averageTimeToAccomplishOrGolden = 0L;
            for (Long time : timesToAccomplishMillisOrGoldenNanos) {
                averageTimeToAccomplishOrGolden += time;
            }
            averageTimeToAccomplishOrGolden /= timesToAccomplishMillisOrGoldenNanos.size();
        }
*/

    }

    public double[] calcGelmanRubinDiagnosticsPSRF() {
        //see http://support.sas.com/documentation/cdl/en/statug/63033/HTML/default/viewer.htm#statug_introbayes_sect008.htm#statug.introbayes.bayesgelman
        //big theta bar := 1/M sigma_{m:1 to M} average(theta_m) where average(theta_m) is 1/n sigma_{t = 1 ot n} theta_{m,t} where theta_{m,t} is the t-th single dimensional sample of m-th chain

        List<Double> b = sampleMeans4timesMultiStat.computeVariance(); //should be multiplied in 'n' the number of taken samples in the current chain
        for (int i = 0; i < b.size(); i++) {
            double n = i + 1;
            Double b0 = b.get(i);
            b.set(i, b0 * n);
        }

        List<Double> w = sampleVariances4timesMultiStat.computeMean();

        double m = sampleVariances4timesMultiStat.numberOfChains();

        double[] potentialScaleReductionFactor = new double[b.size()];
        for (int i = 0; i < b.size(); i++) {
            double n = i + 1;
            double vHat_i = ((n - 1) / n) * w.get(i) + b.get(i) / n;//((m+1)/(n*m))*b.get(i);    //modified due to http://www.people.fas.harvard.edu/~plam/teaching/methods/convergence/convergence_print.pdf
            potentialScaleReductionFactor[i] = Math.sqrt(vHat_i / w.get(i));
        }

        return potentialScaleReductionFactor;

    }

    public void persistGelmanRubinDiagnostics(String path, int numParamDims, String algorithmName) throws FileNotFoundException {
        double[] psrf = calcGelmanRubinDiagnosticsPSRF();

        List<Long> recordedTimePointsInNano = sampleMeans4timesMultiStat.getHallmarkTimeStampsInNano();

        String outputFileName = path + generalInfo(numParamDims, algorithmName) + "-times-gelman";

        PrintStream ps = new PrintStream(new FileOutputStream(outputFileName));

        for (int i = 0; i < psrf.length; i++) {
            //          #index      #time.point(ms)         #mean       #stdErr
            ps.println((i + 1) + "\t" + recordedTimePointsInNano.get(i) + "\t" + psrf[i]);
        }

        ps.close();

    }

    private String generalInfo(int param, String algorithmName/*, int maxAllowedSamplingTime*/) {
        return "param" + param + "-itr" + numRunsPerAlgorithm /*+ "-maxT" + maxAllowedSamplingTime */ + "-" + algorithmName;
    }

    public void persistMeanStdErrForFirstTakenErrSamples(String path, int param, String algorithmName, int numDesiredSamples) throws FileNotFoundException {
        List<Double> means4FirstSamples = errs4FirstSamplesMultiStat.computeMean();
        List<Double> stdErrs4FirstSamples = errs4FirstSamplesMultiStat.computeCorrectedStdErr();

        PrintStream ps;
        String outputFileName = path + generalInfo(param, algorithmName/*, maxAllowedSamplingTime*/) + "-samples" + numDesiredSamples;

        ps = new PrintStream(new FileOutputStream(outputFileName));

        for (int i = 0; i < means4FirstSamples.size(); i++) {
            //          #sample         #mean       #stdErr
            ps.println((i + 1) + "\t" + means4FirstSamples.get(i) + "\t" + stdErrs4FirstSamples.get(i));
        }

        ps.close();
    }

        /*public void persistAutoCorrelationForFirstTakenErrSamples(String path, int param, String algorithmName, int numDesiredSamples) throws FileNotFoundException {
            List<Double> means = effectiveSampleSizeStat.computeMean();
            List<Double> stdErrs = effectiveSampleSizeStat.computeCorrectedVariance();
            PrintStream ps;
            String outputFileName = path + generalInfo(param, algorithmName*//*, maxAllowedSamplingTime*//*) + "-effective-samples" + numDesiredSamples;

            ps = new PrintStream(new FileOutputStream(outputFileName));

            for (int i = 0; i < means.size(); i++) {
                //          #index      #time.point(ms)         #mean       #stdErr
                ps.println((i + 1) + "\t" + means.get(i) + "\t" + stdErrs.get(i));
            }

            ps.close();
        }*/


    public void persistMeanStdErrForTimePoints(String path, int numParamDims, String algorithmName, long maxAllowedSamplingTime) throws FileNotFoundException {
        List<Double> means4TimePoints = errs4timesMultiStat.computeMean();
        List<Double> stdErrs4TimePoints = errs4timesMultiStat.computeCorrectedStdErr();
        List<Long> recordedTimePointsInNano = errs4timesMultiStat.getHallmarkTimeStampsInNano();

        String outputFileName = path + generalInfo(numParamDims, algorithmName/*, maxAllowedSamplingTime*/) + "-times";// + means4TimePoints.size();

        PrintStream ps = new PrintStream(new FileOutputStream(outputFileName));

        for (int i = 0; i < means4TimePoints.size(); i++) {
            //          #index      #time.point(ms)         #mean       #stdErr
            ps.println((i + 1) + "\t" + recordedTimePointsInNano.get(i) + "\t" + means4TimePoints.get(i) + "\t" + stdErrs4TimePoints.get(i));
        }

        ps.close();
    }

    public void persistNumTakenSamplesForTimePoints(String path, int numParamDims, String algorithmName) throws FileNotFoundException {
        List<Double> means4TimePoints = sampleCount4timesMultiStat.computeMean();
        List<Double> stdErrs4TimePoints = sampleCount4timesMultiStat.computeCorrectedStdErr();
        List<Long> recordedTimePointsInNano = sampleCount4timesMultiStat.getHallmarkTimeStampsInNano();

        String outputFileName = path + generalInfo(numParamDims, algorithmName/*, maxAllowedSamplingTime*/) + "-num4times";// + means4TimePoints.size();

        PrintStream ps = new PrintStream(new FileOutputStream(outputFileName));

        for (int i = 0; i < means4TimePoints.size(); i++) {
            //          #index      #time.point(ms)         #mean       #stdErr
            ps.println((i + 1) + "\t" + recordedTimePointsInNano.get(i) + "\t" + means4TimePoints.get(i) + "\t" + stdErrs4TimePoints.get(i));
        }

        ps.close();
    }


    public int numberOfFirstSamples() {
        return errs4FirstSamplesMultiStat.numberOfSamples();
    }


}//end class Stat info
