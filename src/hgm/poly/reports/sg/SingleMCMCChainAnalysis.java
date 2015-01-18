package hgm.poly.reports.sg;

import hgm.poly.diagnostics.*;
import hgm.poly.gm.JointToSampler;
import hgm.poly.gm.RichJointWrapper;
import hgm.poly.sampling.SamplerInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SingleMCMCChainAnalysis {
    public static final int GOLDEN_SAMPLE_COUNT_THRESHOLD = 100;

    List<Double> errForFirstTakenSamples;
    List<Double> errVsTimes;
    List<Long> recordedTimePointsInNano;
//    boolean samplingPerformedInIntendedTimeSuccessfully;
    Long timeToPassGoldenErrorThresholdNanos;    //timeToAccomplishTaskMillisOrGoldenTimeNano
    Long timeToTakeGoldenSampleCountNanos;
    List<Double> sampleMeanVsFirstSamples;   //mean of single dim. samples so far
    List<Double> sampleMeanVsTimes;          //mean of single dim. samples up to the current time slot
    List<Double> sampleVarianceVsFirstSamples;  //var. of single dim. samples so far
    List<Double> sampleVarianceVsTimes;         //var. of single dim. samples so far
    List<Integer> numTakenSamplesVsTimes;
    AutoCorrelationHandler autoCorr;

    //controls:
    boolean computeAutoCorrelation = false;


    ////////////////////////////////////////////////////////////////////////////////////
    public SingleMCMCChainAnalysis (
            DifferenceFromGroundTruthMeasureGenerator differenceFromGroundTruthMeasureGenerator,
//            MeasureOnTheRun<Double[]> errMeasure,
            //double[] groundTruthMeanVector, //of size #dims
            RichJointWrapper jointWrapper,
            JointToSampler samplerMaker,
            int burnedSamples,
            final int numMinDesiredSamples,
            long maxWaitingTimeForTakingDesiredSamplesMillis,
            long minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
            int approxNumTimePointsForWhichErrIsPersisted,
            //
            double goldenErrThreshold) {

        this.timeToPassGoldenErrorThresholdNanos = null;
        this.timeToTakeGoldenSampleCountNanos = null;

//        boolean goldenErrRecorded = false;
//        Long timeToTakeFirstSamplesMillis = null; //not necessarily total time if the time analysis takes more time...
//        samplerMaker.setReusableSample(groundTruthMeanVector);
        //burned samples:
        SamplerInterface sampler = samplerMaker.makeSampler(jointWrapper);
        for (int i = 0; i < burnedSamples; i++) {
            sampler.reusableSample(); //discard samples...
        }

        this.errForFirstTakenSamples = new ArrayList<Double>(numMinDesiredSamples);
        this.recordedTimePointsInNano = new ArrayList<Long>(approxNumTimePointsForWhichErrIsPersisted);
        this.errVsTimes = new ArrayList<Double>(approxNumTimePointsForWhichErrIsPersisted);

        this.sampleMeanVsFirstSamples = new ArrayList<Double>(numMinDesiredSamples);
        this.sampleVarianceVsFirstSamples = new ArrayList<Double>(numMinDesiredSamples);
        this.sampleMeanVsTimes = new ArrayList<Double>(approxNumTimePointsForWhichErrIsPersisted);            //for Gelman and Rubin, think of it as a list of samples where the length of each sample vector is 1
        this.sampleVarianceVsTimes = new ArrayList<Double>(approxNumTimePointsForWhichErrIsPersisted);        //for Gelman and Rubin
        this.numTakenSamplesVsTimes = new ArrayList<Integer>(approxNumTimePointsForWhichErrIsPersisted);

        //trying to take the desired number of samples...
        long absoluteStartTimeMillis = System.currentTimeMillis();
        long absoluteStartTimeNanos = System.nanoTime();

        int savedTimePoints = 0;
        int takenSamples = 0;

//            boolean samplingPerformedInIntendedTimeSuccessfully;

//        AbsDifferenceMeasure errMeasure = new AbsDifferenceMeasure(groundTruthMeanVector);
        MeasureOnTheRun<Double[]> errMeasure = differenceFromGroundTruthMeasureGenerator.generateMeasure();
        MeanMeasure meanMeasure = new MeanMeasure();
        CorrectedVarianceMeasure correctedVarianceMeasure = new CorrectedVarianceMeasure();

        //todo compute again auto correlation
        this.autoCorr =
//                computeAutoCorrelation ?
//                new AutoCorrelationHandler(groundTruthMeanVector, numMinDesiredSamples, numMinDesiredSamples)
//                :
                null;

        boolean desiredNumSamplesTakenConditionA = false;
        boolean desiredSamplingPeriodPassedConditionB = false;
        boolean desiredGoldenErrorThresholdHitConditionC = false;

        for (; ; ) {
            Double[] sample = sampler.reusableSample();
            sample = jointWrapper.reusableQueriedVarValues(sample); //pruneNulls(sample);
            takenSamples++;

            meanMeasure.addNewValue(sample);
            correctedVarianceMeasure.addNewValue(sample);

            errMeasure.addNewValue(sample);
            double runErr = errMeasure.computeMeasure();
            double runSingleDimensionalSampleMean = meanMeasure.computeMeasure();    //the mean of single dimensional samples so far. A single dimensional sample is the average of sample entries.
            double runSingleDimensionalSampleVariance = correctedVarianceMeasure.computeMeasure(); //variance of single dimensional samples so far.

            if (autoCorr != null) autoCorr.addNewValue(sample);

            //first samples:
            if (takenSamples <= numMinDesiredSamples) { //save first samples:
                errForFirstTakenSamples.add(runErr); //error till current taken sample
                sampleMeanVsFirstSamples.add(runSingleDimensionalSampleMean);
                sampleVarianceVsFirstSamples.add(runSingleDimensionalSampleVariance);
            }

            //samples vs. time:
            long nanosFromStart = System.nanoTime() - absoluteStartTimeNanos;
            if ((nanosFromStart >= (double) (((long) savedTimePoints) * minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis * 1000000) / (double) (approxNumTimePointsForWhichErrIsPersisted + 1))
                    && nanosFromStart <= minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis * 1000000) {
                savedTimePoints++;
                errVsTimes.add(runErr);
                recordedTimePointsInNano.add(System.nanoTime() - absoluteStartTimeNanos);
                sampleMeanVsTimes.add(runSingleDimensionalSampleMean);
                sampleVarianceVsTimes.add(runSingleDimensionalSampleVariance);
                numTakenSamplesVsTimes.add(takenSamples);
            }

            //saving time to reach golden Err. threshold:
            if (runErr < goldenErrThreshold && timeToPassGoldenErrorThresholdNanos == null) {
                timeToPassGoldenErrorThresholdNanos = System.nanoTime() - absoluteStartTimeNanos;
                desiredGoldenErrorThresholdHitConditionC = true;
            }

            //saving time to take golden number of samples:
            if (timeToTakeGoldenSampleCountNanos==null && takenSamples>GOLDEN_SAMPLE_COUNT_THRESHOLD) {
                timeToTakeGoldenSampleCountNanos = System.nanoTime() - absoluteStartTimeNanos;
            }


            if (System.currentTimeMillis() - absoluteStartTimeMillis >= minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis &&//savedTimePoints >= approxNumTimePointsForWhichErrIsPersisted &&
                    takenSamples >= numMinDesiredSamples) {
//                samplingPerformedInIntendedTimeSuccessfully = true;
//                System.out.println("successfull after " + takenSamples + " samples.");
//                break;
                desiredNumSamplesTakenConditionA = true;
                desiredSamplingPeriodPassedConditionB = true;
            }

            //successful termination:
            if (desiredNumSamplesTakenConditionA &&
                    desiredSamplingPeriodPassedConditionB &&
                    desiredGoldenErrorThresholdHitConditionC) {
                System.out.println("successfull after " + takenSamples + " samples.");
                break;
            }

            //timeout.
            if (System.currentTimeMillis() - absoluteStartTimeMillis > maxWaitingTimeForTakingDesiredSamplesMillis) {
//                samplingPerformedInIntendedTimeSuccessfully = false;
                System.err.println("time out after taking " + takenSamples + " samples :(" +
                "\nDesired #samples taken: " + desiredNumSamplesTakenConditionA + "\tDesired accuracy threshold reached: " + desiredGoldenErrorThresholdHitConditionC);
                break;
            }

        }//end for loop

//        Long timeToAccomplishTaskMillis = samplingPerformedInIntendedTimeSuccessfully ?
//                System.currentTimeMillis() - absoluteStartTimeMillis
//                :
//                null;

        System.out.println("goldenErrTimeMillis/nanos = " + timeToPassGoldenErrorThresholdNanos + "\n");

    }

    /*public static Double[] pruneNulls(Double[] sample) {
        int nonNullEntryCount = 0;
        for (Double s : sample) {
            if (s != null) nonNullEntryCount++;
        }
        Double[] nonNullEntries = new Double[nonNullEntryCount];
        int i = 0;
        for (Double s : sample) {
            if (s != null) {
                nonNullEntries[i++] = s;
            }
        }
        return nonNullEntries;
    }*/

}
