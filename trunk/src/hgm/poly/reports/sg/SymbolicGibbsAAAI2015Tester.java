package hgm.poly.reports.sg;

import hgm.asve.Pair;
import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;
import hgm.poly.PolynomialFactory;
import hgm.poly.gm.*;
import hgm.poly.sampling.SamplerInterface;
import hgm.poly.sampling.frac.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 18/08/14
 * Time: 7:24 PM
 */
public class SymbolicGibbsAAAI2015Tester {

    public static final String REPORT_PATH_COLLISION_ANALYSIS = "E:/REPORT_PATH_AAAI15/";
    public static final String REPORT_PATH_FERMENTATION_ANALYSIS = "E:/REPORT_PATH_AAAI15/";

    public static void main(String[] args) throws IOException {
        SymbolicGibbsAAAI2015Tester instance = new SymbolicGibbsAAAI2015Tester();
//        instance.collisionAAAI2015Test();
        instance.fermentationAAAI2015Test();
    }

    public void fermentationAAAI2015Test() throws IOException {
        System.out.println("REPORT_PATH_FERMENTATION_ANALYSIS = " + REPORT_PATH_FERMENTATION_ANALYSIS);

        JointToSampler testerSampleMaker =
                //FractionalJointRejectionSampler.makeJointToSampler(1.0);
//                SymbolicFractionalJointGibbsSampler.makeJointToSampler();
//                FractionalJointMetropolisHastingSampler.makeJointToSampler(4.0);
                FractionalJointBaselineGibbsSampler.makeJointToSampler();

        int numSamplesFromTesterToSimulateTrueDistribution = 100000;
        int maxWaitingTimeForTesterToSimulateMillis = 1000 * 60 * 100;
        List<JointToSampler> samplerMakersToBeTested = Arrays.asList(
                SymbolicFractionalJointGibbsSampler.makeJointToSampler(),
                FractionalJointBaselineGibbsSampler.makeJointToSampler(),
//                FractionalJointRejectionSampler.makeJointToSampler(1),
                FractionalJointMetropolisHastingSampler.makeJointToSampler(5.0),
                FractionalJointSelfTunedMetropolisHastingSampler.makeJointToSampler(10, 20, 10)
        );
        int[] numParams = {4, 5, 6, 7, 8, 9, 10};
        int numMinDesiredSamples = 10000;
        int maxWaitingTimeForTakingDesiredSamples = 1000 * 20 * 100;
        int minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis = maxWaitingTimeForTakingDesiredSamples/2;//1000 * 5;
        int approxNumTimePointsForWhichErrIsPersisted = 33;
        int numRuns = 2;
        int burnedSamples = 100;
        int goldenErrThreshold = 0;


        Param2JointWrapper fermentationModelParam2Joint = new Param2JointWrapper() {

            @Override
            public JointWrapper makeJointWrapper(int param) {
//                Double muAlpha = 0.2;
//                Double muBeta = 2.2;
//                Double nuAlpha = -2.0;
//                Double nuBeta = 2.0;
                double minVarLimit = -5;
                double maxVarLimit = 15;

                GraphicalModel bn =
//                        makeFermentationModel(param, 1d, 0.1, 12d);
                        makeSimplifiedFermentationModel(param, 0.1, 12d);


                SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
                Map<String, Double> evidence = new HashMap<String, Double>();

                evidence.put("q", 3d);

                List<String> query = Arrays.asList("l_1", "l_" + (param - 1));
                PiecewiseExpression<Fraction> joint = handler.makeJoint(bn, query, evidence);
                JointWrapper jointWrapper = new JointWrapper(joint, minVarLimit, maxVarLimit);
                return jointWrapper;
            }
        };


        testSamplersPerformanceWrtParameterTimeAndSampleCount(fermentationModelParam2Joint,
                testerSampleMaker,
                numSamplesFromTesterToSimulateTrueDistribution,
                maxWaitingTimeForTesterToSimulateMillis,
                samplerMakersToBeTested, numParams, numMinDesiredSamples,
                maxWaitingTimeForTakingDesiredSamples,
                minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
                approxNumTimePointsForWhichErrIsPersisted, numRuns, burnedSamples, REPORT_PATH_FERMENTATION_ANALYSIS, goldenErrThreshold);

        System.out.println(" That was all the folk for fermentation. ");

    }

    //////////////////////////////////////////////////////////////////////////

    public void collisionAAAI2015Test() throws IOException {
        System.out.println("REPORT_PATH_COLLISION_ANALYSIS = " + REPORT_PATH_COLLISION_ANALYSIS);

        JointToSampler testerSampleMaker = FractionalJointRejectionSampler.makeJointToSampler(1.0);
        int numSamplesFromTesterToSimulateTrueDistribution = 1000;
        int maxWaitingTimeForTesterToSimulateMillis = 1000 * 60;
        List<JointToSampler> samplerMakersToBeTested = Arrays.asList(
                SymbolicFractionalJointGibbsSampler.makeJointToSampler(),
                FractionalJointBaselineGibbsSampler.makeJointToSampler(),
                FractionalJointRejectionSampler.makeJointToSampler(1),
                FractionalJointMetropolisHastingSampler.makeJointToSampler(5.0),
                FractionalJointSelfTunedMetropolisHastingSampler.makeJointToSampler(10, 20, 10)
        );
        int[] numParams = {2, 3};
        int numMinDesiredSamples = 100;
        int maxWaitingTimeForTakingDesiredSamples = 1000 * 20;
        int minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis = 1000 * 5;
        int approxNumTimePointsForWhichErrIsPersisted = 33;
        int numRuns = 2;
        int burnedSamples = 50;
        int goldenErrThreshold = 0;


        Param2JointWrapper collisionModelParam2Joint = new Param2JointWrapper() {

            @Override
            public JointWrapper makeJointWrapper(int param) {
                Double muAlpha = 0.2;
                Double muBeta = 2.2;
                Double nuAlpha = -2.0;
                Double nuBeta = 2.0;
                double minVarLimit = -10;
                double maxVarLimit = 10;

                GraphicalModel bn = makeCollisionModel(param,
                        muAlpha, muBeta, nuAlpha, nuBeta);//paramDataCount2DataGenerator.createJointGenerator(param);


                SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
                Map<String, Double> evidence = new HashMap<String, Double>();

                evidence.put("p_t", 3d);        //todo...    ???
                evidence.put("m_1", 2d);
//        evidence.put("v_2", 0.2d);

                List<String> query = Arrays.asList("v_1", "v_" + (param - 1));
                PiecewiseExpression<Fraction> joint = handler.makeJoint(bn, query, evidence);
                JointWrapper jointWrapper = new JointWrapper(joint, minVarLimit, maxVarLimit);
                return jointWrapper;
            }
        };


        testSamplersPerformanceWrtParameterTimeAndSampleCount(collisionModelParam2Joint,
                testerSampleMaker,
                numSamplesFromTesterToSimulateTrueDistribution,
                maxWaitingTimeForTesterToSimulateMillis,
                samplerMakersToBeTested, numParams, numMinDesiredSamples,
                maxWaitingTimeForTakingDesiredSamples,
                minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
                approxNumTimePointsForWhichErrIsPersisted, numRuns, burnedSamples, REPORT_PATH_COLLISION_ANALYSIS, goldenErrThreshold);

        System.out.println(" That was all the folk. ");

    }

    private void testSamplersPerformanceWrtParameterTimeAndSampleCount(
            Param2JointWrapper paramToJointWrapper, //model
            JointToSampler testerSampleMaker,  //used as baseline
            int numSamplesFromTesterToSimulateTrueDistribution,
            long maxWaitingTimeForTesterToSimulateMillis,
            List<JointToSampler> samplerMakersToBeTested,
            int[] numParams, // number of states in a dynamic model, or number of objects in collision model, ..., parameter space dimension
            int numMinDesiredSamples,     //used for error vs. #samples diagram
            long maxWaitingTimeForTakingDesiredSamples, //if sampling takes more than this, sampling would be terminated without an answer
            long minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis, //used for error vs. time diagram
            int approxNumTimePointsForWhichErrIsPersisted,
            int numRuns,
            int burnedSamples, //used for both tester and tested algorithms
            String outputDirectoryPath,
            double goldenErrThreshold) throws IOException {


        TotalTimeKeeper timeKeeper = new TotalTimeKeeper(samplerMakersToBeTested, numParams, outputDirectoryPath);

//        AlgorithmDeathLimitKeeper testedAlgsDeathKeeper = new AlgorithmDeathLimitKeeper(samplerMakersToBeTested);  todo un-comment if necessary


        //note: this array should be sorted
        for (int param : numParams) {
            System.out.println(".......\nparam = " + param);

            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

            JointWrapper jointWrapper = paramToJointWrapper.makeJointWrapper(param);
            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

            double[] groundTruthMeans;
            try {
                groundTruthMeans = computeGroundTruthMean(jointWrapper, testerSampleMaker,
                        numSamplesFromTesterToSimulateTrueDistribution, maxWaitingTimeForTesterToSimulateMillis);
                System.out.println("{{{groundTruthMeans = " + Arrays.toString(groundTruthMeans) + "}}}");
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("NO GROUND TRUTH TAKEN! TERMINATED....");
                continue;
            }

            //Analysis of tested algorithms:
            for (JointToSampler samplerMaker : samplerMakersToBeTested) {
//                if (!testedAlgsDeathKeeper.algorithmWillDie(samplerMaker, param, numObservedDataPoints)) {
                StatInfo statInfo = meansAndStdErrors(numRuns,
                        groundTruthMeans,
                        jointWrapper,
                        samplerMaker,
                        burnedSamples,
                        numMinDesiredSamples,
                        maxWaitingTimeForTakingDesiredSamples,
                        minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
                        approxNumTimePointsForWhichErrIsPersisted,
                        goldenErrThreshold);
                statInfo.persistMeanStdErrForFirstTakenSamples(outputDirectoryPath, param, samplerMaker.getName(), numMinDesiredSamples);
                statInfo.persistMeanStdErrForTimePoints(outputDirectoryPath, param, samplerMaker.getName(), maxWaitingTimeForTakingDesiredSamples);

//                if (statInfo.timeToTakeFirstSamplesOrGoldenTime != null) { //did not die        //todo!!!!!!!!!!!!!!!!!!!!
//                    timeKeeper.persist(param, samplerMaker.getName(), statInfo.timeToTakeFirstSamplesOrGoldenTime);
//                } else { //died
//                    throw new RuntimeException("died");
//                    testedAlgsDeathKeeper.recordDeath(samplerMaker, param); //todo uncomment if necessary
//                }

                System.out.println(samplerMaker.getName() + ".timeN/GOLD = " + statInfo.timeToTakeFirstSamplesOrGoldenTime/*totalProcessTimeMillis*/ + "\t\tsamples=" + statInfo.means4FirstSamples.size());

//                } else {
//                    System.err.println(samplerMaker.getName() + " skipped...");
//                }
            }
//                FunctionVisualizer.visualize(stdErr, 0, numSamples, 1, statType + " #dim:" + numDims + " #cnstrnt:" + numConstraints);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public StatInfo meansAndStdErrors(
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

        double rootNumRuns = Math.sqrt(numRunsPerAlgorithm);
        Double[] meanFirstSampledErrs = new Double[numMinDesiredSamples]; //E{X}:: {E{alg_1_err1, alg2_err1, ...algN_err1}, E{alg_1_err2, alg2_err2, ...algN_err2},...}
        Arrays.fill(meanFirstSampledErrs, 0d);
        double[] exp2FirstSampledErrs = new double[numMinDesiredSamples]; //E{X^2}
        int minFirstSamplesTakenPerIterations = numMinDesiredSamples;

        List<Long> hallmarkTimeStampsInNano = null;
        List<Double> meanErrForTimes = null;
        double[] exp2ErrForTimes = null;

        List<Long> timesToAccomplishMillisOrGoldenNanos = new ArrayList<Long>(numRunsPerAlgorithm);

        for (int runNum = 0; runNum < numRunsPerAlgorithm; runNum++) {
            System.out.println("ALG: " + samplerMaker.getName() + " -- ITR. = " + runNum);

            Err4Samples_Err4times_Times err4Samples_err4times_times = errorVsSamplesAndTime(groundTruthMeanVector, joint, samplerMaker, burnedSamples,
                    numMinDesiredSamples, maxWaitingTimeForTakingDesiredSamplesMillis,
                    minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis, approxNumTimePointsForWhichErrIsPersisted, goldenErrThreshold);

            Long accomplishTimeMSOrGoldenTimeNS = err4Samples_err4times_times.timeToAccomplishTaskMillisOrGoldenTimeNano;
            if (accomplishTimeMSOrGoldenTimeNS != null)
                timesToAccomplishMillisOrGoldenNanos.add(accomplishTimeMSOrGoldenTimeNS);

            //means and std-errors for "errors for first samples":
            List<Double> errList = err4Samples_err4times_times.errForFirstTakenSamples; //err. for first taken samples
            minFirstSamplesTakenPerIterations = Math.min(minFirstSamplesTakenPerIterations, errList.size());
            for (int i = 0; i < minFirstSamplesTakenPerIterations; i++) {   //the entries more than this min... are useless, discarded eventually
                Double currentErrForFirstTakenSamples = errList.get(i);
                meanFirstSampledErrs[i] = meanFirstSampledErrs[i] + (currentErrForFirstTakenSamples / (double) numRunsPerAlgorithm);
                exp2FirstSampledErrs[i] = exp2FirstSampledErrs[i] + (currentErrForFirstTakenSamples * currentErrForFirstTakenSamples) / (double) numRunsPerAlgorithm;
            }

            //means and std-errors for "errors in time stamps":
            List<Long> recordedTimePointsInNano = err4Samples_err4times_times.recordedTimePointsInNano;
            List<Double> errVsTimes = err4Samples_err4times_times.errVsTimes;
            if (hallmarkTimeStampsInNano == null) {
                hallmarkTimeStampsInNano = recordedTimePointsInNano; //so the times points of the first algorithm-run are the hall marks...
                meanErrForTimes = errVsTimes; //means of a single elements = same single elements\
                exp2ErrForTimes = new double[meanErrForTimes.size()];
                for (int i = 0; i < errVsTimes.size(); i++) {
                    Double errVsTime = errVsTimes.get(i);
                    exp2ErrForTimes[i] = errVsTime * errVsTime;
                }
            } else if (!recordedTimePointsInNano.isEmpty()) { //E[X], E[X^2] of 'means vs Times' should be updated
                int index2 = 0;
                for (int index1 = 0; index1 < hallmarkTimeStampsInNano.size(); index1++) {
                    Long hallMarkTime = hallmarkTimeStampsInNano.get(index1);
                    for (int i = index2; i < recordedTimePointsInNano.size(); i++) {
                        Long newT1 = recordedTimePointsInNano.get(i);
                        Long newT2 = (i == recordedTimePointsInNano.size() - 1) ? newT1 : recordedTimePointsInNano.get(i + 1);
                        long deltaT1 = Math.abs(newT1 - hallMarkTime);
                        long deltaT2 = Math.abs(newT2 - hallMarkTime);
                        if (deltaT1 <= deltaT2) {
                            index2 = i; //so that next time search is started from here
                            break;
                        }
                    }
//                    long nearestTime = recordedTimePointsInNano.get(index2);
                    Double errAssociatedWithNearestTime = errVsTimes.get(index2);
                    meanErrForTimes.set(index1, ((meanErrForTimes.get(index1) * runNum) + errAssociatedWithNearestTime) / (double) (runNum + 1));//mean is updated with the closest new time
                    exp2ErrForTimes[index1] = ((exp2ErrForTimes[index1] * runNum) + errAssociatedWithNearestTime * errAssociatedWithNearestTime) / (double) (runNum + 1);
                }
            }

        } //end alg. run num.

        if (meanFirstSampledErrs.length != minFirstSamplesTakenPerIterations) {
            //prune the useless end of the mean array:
            Double[] resizeArray = new Double[minFirstSamplesTakenPerIterations];
            System.arraycopy(meanFirstSampledErrs, 0, resizeArray, 0, minFirstSamplesTakenPerIterations);
            meanFirstSampledErrs = resizeArray;
        }


        Double[] stdErrs4FirstSamples = new Double[minFirstSamplesTakenPerIterations];
        for (int i = 0; i < minFirstSamplesTakenPerIterations; i++) {
            stdErrs4FirstSamples[i] = Math.sqrt(exp2FirstSampledErrs[i] - meanFirstSampledErrs[i] * meanFirstSampledErrs[i]) / rootNumRuns;
        }

        Double[] stdErrs4Times = new Double[meanErrForTimes.size()];
        for (int i = 0; i < meanErrForTimes.size(); i++) {
            double mean = meanErrForTimes.get(i);
            stdErrs4Times[i] = Math.sqrt(exp2ErrForTimes[i] - mean * mean) / rootNumRuns;
        }

        Long averageTimeToAccomplishOrGolden = null;//todo std err may be calculated as well.
        if (!timesToAccomplishMillisOrGoldenNanos.isEmpty()) {
            averageTimeToAccomplishOrGolden = 0L;
            for (Long time : timesToAccomplishMillisOrGoldenNanos) {
                averageTimeToAccomplishOrGolden += time;
            }
            averageTimeToAccomplishOrGolden /= timesToAccomplishMillisOrGoldenNanos.size();
        }

        return new StatInfo(Arrays.asList(meanFirstSampledErrs), Arrays.asList(stdErrs4FirstSamples), meanErrForTimes, Arrays.asList(stdErrs4Times), hallmarkTimeStampsInNano, averageTimeToAccomplishOrGolden, numRunsPerAlgorithm);

        /*    //mean and stdErr for all Iterations
            double mean = 0d;  //E[X]
            double ex2 = 0;  //E[X^2]
            for (int fId = 0; fId < numFs; fId++) {
                double x = runningErrorPerF[fId];
                mean += (x / (double) numFs);
                ex2 += (x * x / (double) numFs);
            }
            double stdErr = root(ex2 - mean * mean) / rootNumRuns;
*/

    }

    public Err4Samples_Err4times_Times errorVsSamplesAndTime(
            double[] groundTruthMeanVector, //of size #dims
            JointWrapper db,
            JointToSampler samplerMaker,
            int burnedSamples,
            final int numMinDesiredSamples,
            long maxWaitingTimeForTakingDesiredSamplesMillis,
            long minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
            int approxNumTimePointsForWhichErrIsPersisted,
            //
            double goldenErrThreshold
    ) {
        Long goldenErrTimeMillisOrNano = null;
        boolean goldenErrRecorded = false;
//        Long timeToTakeFirstSamplesMillis = null; //not necessarily total time if the time analysis takes more time...
//        samplerMaker.setReusableSample(groundTruthMeanVector);
        //burned samples:
        SamplerInterface sampler = samplerMaker.makeSampler(db.joint, db.minVarLimit, db.maxVarLimit);
        for (int i = 0; i < burnedSamples; i++) {
            sampler.reusableSample(); //discard samples...
        }

        double[] runningAccumulatedSample = null; //since maybe I do not know the dim yet...(if no burned sample is taken)

        List<Double> errVsFirstSamples = new ArrayList<Double>(numMinDesiredSamples);
        List<Long> recordedTimePointsInNano = new ArrayList<Long>(approxNumTimePointsForWhichErrIsPersisted);
        List<Double> errVsTimes = new ArrayList<Double>(approxNumTimePointsForWhichErrIsPersisted);

        //trying to take the desired number of samples...
        long absoluteStartTimeMillis = System.currentTimeMillis();
        long absoluteStartTimeNanos = System.nanoTime();

        int savedTimePoints = 0;
        int takenSamples = 0;

        boolean samplingPerformedInIntendedTimeSuccessfully;

        for (; ; ) {
            Double[] sample = sampler.reusableSample();
            sample = pruneNulls(sample);
            takenSamples++;

            if (runningAccumulatedSample == null) {
                runningAccumulatedSample = new double[sample.length];
            }

            for (int i = 0; i < sample.length; i++) {
                runningAccumulatedSample[i] = runningAccumulatedSample[i] + sample[i];
            }

            //sum_i (absolute difference of (average of taken_sample_i and ground_truth_i)):
            double runErr = 0;  //running error
            for (int i = 0; i < sample.length; i++) {
                runErr += Math.abs((runningAccumulatedSample[i] / (double) takenSamples) - groundTruthMeanVector[i]);
            }
            runErr /= (double) sample.length;

            //first samples:
            if (takenSamples <= numMinDesiredSamples) { //save first samples:
                errVsFirstSamples.add(runErr); //error till current taken sample
            }

            //samples vs. time:
            long nanosFromStart = System.nanoTime() - absoluteStartTimeNanos;
            if ((nanosFromStart >= (double) (((long) savedTimePoints) * minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis * 1000000) / (double) (approxNumTimePointsForWhichErrIsPersisted + 1))
                    && nanosFromStart <= minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis * 1000000) {
                savedTimePoints++;
                errVsTimes.add(runErr);
                recordedTimePointsInNano.add(System.nanoTime() - absoluteStartTimeNanos);
            }

            //saving golden Err. time:
            if (!goldenErrRecorded && runErr < goldenErrThreshold) {
                goldenErrRecorded = true;
                goldenErrTimeMillisOrNano = System.nanoTime() - absoluteStartTimeNanos;//System.currentTimeMillis() - absoluteStartTimeMillis;
                System.out.println("sampling successfully terminated due to reaching golden error rate: " + goldenErrRecorded + " -in " + goldenErrTimeMillisOrNano + "(ns)");
                samplingPerformedInIntendedTimeSuccessfully = true;
                break;
            }

            //successful termination:
            if (
                    System.currentTimeMillis() - absoluteStartTimeMillis >= minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis &&//savedTimePoints >= approxNumTimePointsForWhichErrIsPersisted &&
                            takenSamples >= numMinDesiredSamples) {
                samplingPerformedInIntendedTimeSuccessfully = true;
                System.out.println("successfull after " + takenSamples + " samples.");
                break;
            }

            //timeout.
            if (System.currentTimeMillis() - absoluteStartTimeMillis > maxWaitingTimeForTakingDesiredSamplesMillis) {
                samplingPerformedInIntendedTimeSuccessfully = false;
//                System.err.println("time out after taking " + takenSamples + " samples.");
                break;
            }

        }//end for loop

        Long timeToAccomplishTaskMillis = samplingPerformedInIntendedTimeSuccessfully ?
                System.currentTimeMillis() - absoluteStartTimeMillis
                :
                null;

        System.out.println("goldenErrTimeMillis/nanos = " + goldenErrTimeMillisOrNano + "\n");
        return new Err4Samples_Err4times_Times(errVsFirstSamples, errVsTimes, recordedTimePointsInNano, samplingPerformedInIntendedTimeSuccessfully, goldenErrTimeMillisOrNano/*timeToAccomplishTaskMillis*/);
    }

    class Err4Samples_Err4times_Times {
        List<Double> errForFirstTakenSamples;
        List<Double> errVsTimes;
        List<Long> recordedTimePointsInNano;
        boolean samplingPerformedInIntendedTimeSuccessfully;
        Long timeToAccomplishTaskMillisOrGoldenTimeNano;

        Err4Samples_Err4times_Times(List<Double> errForFirstTakenSamples, List<Double> errVsTimes, List<Long> recordedTimePointsInNano,
                                    boolean samplingPerformedInIntendedTimeSuccessfully, Long timeToAccomplishTaskMillisOrGoldenTimeNano) {
            this.errForFirstTakenSamples = errForFirstTakenSamples;
            this.errVsTimes = errVsTimes;
            this.recordedTimePointsInNano = recordedTimePointsInNano;
            this.samplingPerformedInIntendedTimeSuccessfully = samplingPerformedInIntendedTimeSuccessfully;
            this.timeToAccomplishTaskMillisOrGoldenTimeNano = timeToAccomplishTaskMillisOrGoldenTimeNano;
        }
    }


    class StatInfo {
        //general info:

//        int numF;
//        double[] groundTruthMeans; //this is an array of size #F not persisted but kept in case ground truth is calculated for the first time.

        //calculated info:
        List<Double> means4FirstSamples;
        List<Double> stdErrs4FirstSamples;
        List<Double> means4TimePoints;
        List<Double> stdErrs4TimePoints;
        List<Long> recordedTimePointsInNano;
        //        Long totalProcessTimeMillis;
        Long timeToTakeFirstSamplesOrGoldenTime;

        int numIterationsForEachAlgorithm;

        public StatInfo(List<Double> meanOfErrorVsFirstSamples, List<Double> stdErrOfErrorVsFirstSamples,
                        List<Double> meanOfErrorVsTime, List<Double> stdErrOfErrorVsTime, List<Long> recordedTimePointsInNano,
                        Long timeToTakeFirstSamplesOrGoldenTime, int numIterationsForEachAlgorithm) {
            this.numIterationsForEachAlgorithm = numIterationsForEachAlgorithm;
//            this.numF = groundTruthMeans.length;

            this.means4FirstSamples = meanOfErrorVsFirstSamples;
            this.stdErrs4FirstSamples = stdErrOfErrorVsFirstSamples;

            this.means4TimePoints = meanOfErrorVsTime;
            this.stdErrs4TimePoints = stdErrOfErrorVsTime;
            this.recordedTimePointsInNano = recordedTimePointsInNano;

            this.timeToTakeFirstSamplesOrGoldenTime = timeToTakeFirstSamplesOrGoldenTime;

            if (means4FirstSamples.size() != stdErrs4FirstSamples.size())
                throw new RuntimeException("size mismatch between |mean|= " + means4FirstSamples.size() + " and |stdErr|= " + stdErrs4FirstSamples.size());
            if ((means4TimePoints.size() != stdErrs4TimePoints.size()) || (means4TimePoints.size() != this.recordedTimePointsInNano.size()))
                throw new RuntimeException("size mismatch");

        }

        private String generalInfo(int param, String algorithmName/*, int maxAllowedSamplingTime*/) {
            return "param" + param + "-itr" + numIterationsForEachAlgorithm /*+ "-maxT" + maxAllowedSamplingTime */ + "-" + algorithmName;
        }

        public void persistMeanStdErrForFirstTakenSamples(String path, int param, String algorithmName, int numDesiredSamples) throws FileNotFoundException {
            PrintStream ps;
            String outputFileName = path + generalInfo(param, algorithmName/*, maxAllowedSamplingTime*/) + "-samples" + numDesiredSamples;

            ps = new PrintStream(new FileOutputStream(outputFileName));

            for (int i = 0; i < means4FirstSamples.size(); i++) {
                //          #sample         #mean       #stdErr
                ps.println((i + 1) + "\t" + means4FirstSamples.get(i) + "\t" + stdErrs4FirstSamples.get(i));
            }

            ps.close();
        }


        public void persistMeanStdErrForTimePoints(String path, int numParamDims, String algorithmName, long maxAllowedSamplingTime) throws FileNotFoundException {
            String outputFileName = path + generalInfo(numParamDims, algorithmName/*, maxAllowedSamplingTime*/) + "-times";// + means4TimePoints.size();

            PrintStream ps = new PrintStream(new FileOutputStream(outputFileName));

            for (int i = 0; i < means4TimePoints.size(); i++) {
                //          #index      #time.point(ms)         #mean       #stdErr
                ps.println((i + 1) + "\t" + recordedTimePointsInNano.get(i) /*/ 1000000*/ + "\t" + means4TimePoints.get(i) + "\t" + stdErrs4TimePoints.get(i));
            }

            ps.close();
        }
    }//end class Stat info

    private class TotalTimeKeeper {
        String outputDirectoryPath;
        Map<String/*algorithm*/, SortedSet<Pair<Integer /*data*/, Long /*totalTime*/>>> alg2dataTime;
        Map<String/*algorithm*/, SortedSet<Pair<Integer /*dims*/, Long /*totalTime*/>>> alg2dimsTime;

        public TotalTimeKeeper(JointToSampler[] samplers, String outputDirectoryPath) {
            this.outputDirectoryPath = outputDirectoryPath;

            alg2dataTime = new HashMap<String, SortedSet<Pair<Integer, Long>>>(samplers.length);
            alg2dimsTime = new HashMap<String, SortedSet<Pair<Integer, Long>>>(samplers.length);

            for (JointToSampler sampler : samplers) {
                alg2dataTime.put(sampler.getName(), new TreeSet<Pair<Integer, Long>>());
            }
        }

        int[] dimsArray;
        //        int[] dataArray;
        String[] samplerNames;
        Map<String /*alg*/, Long/*time*/>[] dimIndexDataIndexAlgTime;

        public TotalTimeKeeper(List<JointToSampler> samplers, int[] dimsArray/*, int[] dataArray*/, String outputDirectoryPath) {
            this.outputDirectoryPath = outputDirectoryPath;

            this.dimsArray = new int[dimsArray.length];
//            this.dataArray = new int[dataArray.length];
            System.arraycopy(dimsArray, 0, this.dimsArray, 0, dimsArray.length);
//            System.arraycopy(dataArray, 0, this.dataArray, 0, dataArray.length);
            Arrays.sort(this.dimsArray);
//            Arrays.sort(this.dataArray);

            dimIndexDataIndexAlgTime = new HashMap[dimsArray.length];//[dataArray.length];

            this.samplerNames = new String[samplers.size()];
            for (int i = 0; i < samplers.size(); i++) {
                this.samplerNames[i] = samplers.get(i).getName();
            }

            for (int i = 0; i < dimsArray.length; i++) {
//                for (int j = 0; j < dataArray.length; j++) {
                dimIndexDataIndexAlgTime[i] = new HashMap<String, Long>(samplers.size());
//                }
            }
        }

        public void persist(int param, String samplerName, Long timeMillis) throws FileNotFoundException {
            int dimIndex = Arrays.binarySearch(dimsArray, param);
//            int dataIndex = Arrays.binarySearch(dataArray, data);
            if (dimIndexDataIndexAlgTime[dimIndex].put(samplerName, timeMillis) != null) {
                System.err.println("for dim: " + param + ", " + samplerName + " already exists! and will be replaced");
                System.out.println("dimsArray = " + Arrays.toString(dimsArray));
//                System.out.println("dataArray = " + Arrays.toString(dataArray));
            }
//            persistDimFix(samplerName, dimIndex);
            persistDataFix(samplerName/*, dataIndex*/);
        }

//        private void persistDimFix(String samplerName, int dimIndex) throws FileNotFoundException {
//            int dim = dimsArray[dimIndex];
//
//            List<Integer> dataList = new ArrayList<Integer>();
//            List<Long> timeList = new ArrayList<Long>();
//            for (int dataIndex = 0; dataIndex < dataArray.length; dataIndex++) {
//                Long time = dimIndexDataIndexAlgTime[dimIndex][dataIndex].get(samplerName);
//                if (time == null) continue;
//                int data = dataArray[dataIndex];
//                dataList.add(data);
//                timeList.add(time);
//            }
//            persistOneEntryFixed(samplerName, "dim", dim, dataList, timeList);
//        }

        private void persistDataFix(String samplerName/*, int dataIndex*/) throws FileNotFoundException {
//            int data = dataArray[dataIndex];

            List<Integer> dimList = new ArrayList<Integer>();
            List<Long> timeList = new ArrayList<Long>();
            for (int dimIndex = 0; dimIndex < dimsArray.length; dimIndex++) {
                Long time = dimIndexDataIndexAlgTime[dimIndex].get(samplerName);
                if (time == null) continue;
                int dim = dimsArray[dimIndex];
                dimList.add(dim);
                timeList.add(time);
            }
            persistInner(samplerName, dimList, timeList);
        }

        private void persistInner(String samplerName, List<Integer> elementList, List<Long> timeList) throws FileNotFoundException {
            String outputFileName = this.outputDirectoryPath + "-" + samplerName;

            PrintStream ps = new PrintStream(new FileOutputStream(outputFileName));

            int n = elementList.size();
            if (n != timeList.size()) throw new RuntimeException("size mismatch");
            for (int i = 0; i < n; i++) {
                ps.println(elementList.get(i) + "\t" + timeList.get(i));
            }

            ps.close();
        }

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public double[] computeGroundTruthMean(
            JointWrapper jointWrapper, JointToSampler samplerMaker,
            final int numDesiredSamples,
            long maxWaitingTimeForTakingDesiredSamplesMillis //in ms
    ) {
        double[] mean = null;
        int numTakenSamples = 0;

        ///////////////////////////////////////////////////////////////////////
        SamplerInterface sampler = samplerMaker.makeSampler(jointWrapper.joint, jointWrapper.minVarLimit, jointWrapper.maxVarLimit);
        ///////////////////////////////////////////////////////////////////////

        long t1 = System.currentTimeMillis();
        for (int sampleCount = 0; sampleCount < numDesiredSamples; sampleCount++) {
            Double[] sample = sampler.reusableSample();
            sample = pruneNulls(sample);

            if (mean == null) {
                mean = new double[sample.length];
            }

            for (int j = 0; j < mean.length; j++) {
                mean[j] = mean[j] + sample[j];
            }
            numTakenSamples++;
            if (System.currentTimeMillis() - t1 > maxWaitingTimeForTakingDesiredSamplesMillis) break;
        }

        System.out.println("num. Ground. Truth. TakenSamples = " + numTakenSamples + "\t in time <= " + maxWaitingTimeForTakingDesiredSamplesMillis + "(ms)");

        for (int j = 0; j < mean.length; j++) {
            mean[j] = mean[j] / (double) numTakenSamples;
        }

        return mean;
    }

    private Double[] pruneNulls(Double[] sample) {
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
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public GraphicalModel makeSimplifiedFermentationModel(int n /*num colliding objects*/,
                                                          Double minLactoseAlpha, Double maxInitialLactoseBeta) {
//                                                 double minVarLimit, double maxVarLimit,
//                                                 JointToSampler jointToSampler) {
        // l_1 --> l_2 --> ... --> l_n
        //  |       |            |
        //  \_______\____________\____q //average ph

        String[] vars = new String[3 * n + 1];
        for (int i = 0; i < n; i++) {
            vars[3 * i] = "l_" + (i + 1);     // lactose at time step i
            vars[3 * i + 1] = "k_" + (i + 1); // not used...
            vars[3 * i + 2] = "p_" + (i + 1); // not used...
        }
        vars[3 * n] = "q";

        PolynomialFactory factory = new PolynomialFactory(vars);
        Distributions dBank = new Distributions(factory);

        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();

        PiecewiseExpression<Fraction>[] lactoseFs = new PiecewiseExpression[n];
//        PiecewiseExpression<Fraction>[] pHFs = new PiecewiseExpression[n];

        String averagePH = "";
        for (int i = 0; i < n; i++) {
            lactoseFs[i] = i == 0 ?
                    dBank.createUniformDistributionFraction("l_1", minLactoseAlpha.toString(), maxInitialLactoseBeta.toString())
                    : dBank.createUniformDistributionFraction("l_" + (i + 1), minLactoseAlpha.toString(), "l_" + i + "^(1)");
            averagePH += ("l_" + (i + 1) + "^(1) +");
        }
        averagePH = averagePH.substring(0, averagePH.length() - 1); //removing last "+"

        Fraction averagePhF = factory.makeFraction(averagePH, "" + n); // m_1^(1)*v_1^(1) + m_2^(1)*v_2^(1) + ...

        for (int i = 0; i < n; i++) {
            bn.addFactor(new StochasticVAFactor("l_" + (i + 1), lactoseFs[i])); //mass
        }

        bn.addFactor(new DeterministicFactor("q", averagePhF)); //total momentum
        return bn;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public GraphicalModel makeFermentationModel(int n /*num colliding objects*/,
                                                Double pDistributionParam,
                                                Double minLactoseAlpha, Double maxInitialLactoseBeta) {
//                                                 double minVarLimit, double maxVarLimit,
//                                                 JointToSampler jointToSampler) {
        // a_1 --> a_2 --> ... --> a_n
        //  |       |            |
        //  p_1     p_2          p_n
        //   \______\____________\____q //average ph

        String[] vars = new String[3 * n + 1];
        for (int i = 0; i < n; i++) {
            vars[3 * i] = "l_" + (i + 1);     //lactose at time step i
            vars[3 * i + 1] = "k_" + (i + 1); //"K. Marxianus" at time step i      //todo not used...
            vars[3 * i + 2] = "p_" + (i + 1); //observation (of pH) at time step i
        }
        vars[3 * n] = "q";

        PolynomialFactory factory = new PolynomialFactory(vars);
        Distributions dBank = new Distributions(factory);

        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();

        PiecewiseExpression<Fraction>[] lactoseFs = new PiecewiseExpression[n];
        PiecewiseExpression<Fraction>[] pHFs = new PiecewiseExpression[n];
//        PiecewiseExpression<Fraction>[] kMarxianusFs = new PiecewiseExpression[n];

//        Fraction[] momentaF = new Fraction[n];
        String averagePH = "";
//        String totalMassFormula = "";
//        double c = 1; //just a bound
        for (int i = 0; i < n; i++) {
            lactoseFs[i] = i == 0 ?
                    dBank.createUniformDistributionFraction("l_1", minLactoseAlpha.toString(), maxInitialLactoseBeta.toString())
                    : dBank.createUniformDistributionFraction("l_" + (i + 1), minLactoseAlpha.toString(), "l_" + i + "^(1)");
            pHFs[i] = dBank.createUniformDistributionFraction("p_" + (i + 1), "l_" + (i + 1) + "^(1) + " + (-pDistributionParam), "l_" + (i + 1) + "^(1) + " + pDistributionParam);//TruncatedNormalDistributionFraction("p_"  + (i + 1), )//todo test other things... E.g., TRIANGULAR
//            momentaF[i] = factory.makeFraction("m_" + (i + 1) + "^(1) * v_" + (i + 1) + "^(1)");
            averagePH += ("p_" + (i + 1) + "^(1) +");
//            totalMassFormula += ("m_" + (i + 1) + "^(1) +");
        }
        averagePH = averagePH.substring(0, averagePH.length() - 1); //removing last "+"
//        totalMassFormula = totalMassFormula.substring(0, totalMassFormula.length() - 1); //removing last "+"

        Fraction averagePhF = factory.makeFraction(averagePH, "" + n); // m_1^(1)*v_1^(1) + m_2^(1)*v_2^(1) + ...
//        Fraction mtF = factory.makeFraction(totalMassFormula);
//        Fraction vtF = factory.makeFraction("[p_t^(1)]/[m_t^(1)]");

        for (int i = 0; i < n; i++) {
            bn.addFactor(new StochasticVAFactor("l_" + (i + 1), lactoseFs[i])); //mass
            bn.addFactor(new StochasticVAFactor("p_" + (i + 1), pHFs[i]));
//            bn.addFactor(new DeterministicFactor("p_" + (i + 1), momentaF[i])); //momentum
        }

        bn.addFactor(new DeterministicFactor("q", averagePhF)); //total momentum
//        bn.addFactor(new DeterministicFactor("m_t", mtF)); //total mass (after collision)
//        bn.addFactor(new DeterministicFactor("v_t", vtF)); //total velocity (after collision)
        return bn;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public GraphicalModel makeCollisionModel(int n /*num colliding objects*/,
                                             Double muAlpha, Double muBeta,
                                             Double nuAlpha, Double nuBeta) {
//                                                 double minVarLimit, double maxVarLimit,
//                                                 JointToSampler jointToSampler) {
        // m_1      v_1   v_n     m_2
        //   \__p_1__/    \__p_n__/
        //       \_____p_t____/


        String[] vars = new String[3 * n + 3];
        for (int i = 0; i < n; i++) {
            vars[3 * i] = "m_" + (i + 1);     //mass of i-th object
            vars[3 * i + 1] = "v_" + (i + 1); //velocity of i-th object
            vars[3 * i + 2] = "p_" + (i + 1); //momentum of i-th object
        }
        vars[3 * n] = "m_t"; //total mass
        vars[3 * n + 1] = "v_t"; //total velocity
        vars[3 * n + 2] = "p_t"; //total momentum


        PolynomialFactory factory = new PolynomialFactory(vars);
        Distributions dBank = new Distributions(factory);

        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();

        PiecewiseExpression<Fraction>[] massesF = new PiecewiseExpression[n];
        PiecewiseExpression<Fraction>[] velocitiesF = new PiecewiseExpression[n];
        Fraction[] momentaF = new Fraction[n];
        String totalMomentumFormula = "";
        String totalMassFormula = "";
        for (int i = 0; i < n; i++) {
            massesF[i] = dBank.createUniformDistributionFraction("m_" + (i + 1), muAlpha.toString(), muBeta.toString());
            velocitiesF[i] = i == 0 ? dBank.createUniformDistributionFraction("v_1", nuAlpha.toString(), nuBeta.toString())
                    : dBank.createUniformDistributionFraction("v_" + (i + 1), nuAlpha.toString(), "v_" + i + "^(1)");
            momentaF[i] = factory.makeFraction("m_" + (i + 1) + "^(1) * v_" + (i + 1) + "^(1)");
            totalMomentumFormula += ("p_" + (i + 1) + "^(1) +");
            totalMassFormula += ("m_" + (i + 1) + "^(1) +");
        }
        totalMomentumFormula = totalMomentumFormula.substring(0, totalMomentumFormula.length() - 1); //removing last "+"
        totalMassFormula = totalMassFormula.substring(0, totalMassFormula.length() - 1); //removing last "+"

        Fraction ptF = factory.makeFraction(totalMomentumFormula); // m_1^(1)*v_1^(1) + m_2^(1)*v_2^(1) + ...
        Fraction mtF = factory.makeFraction(totalMassFormula);
        Fraction vtF = factory.makeFraction("[p_t^(1)]/[m_t^(1)]");

        for (int i = 0; i < n; i++) {
            bn.addFactor(new StochasticVAFactor("m_" + (i + 1), massesF[i])); //mass
            bn.addFactor(new StochasticVAFactor("v_" + (i + 1), velocitiesF[i])); //mass 2
            bn.addFactor(new DeterministicFactor("p_" + (i + 1), momentaF[i])); //momentum
        }

        bn.addFactor(new DeterministicFactor("p_t", ptF)); //total momentum
        bn.addFactor(new DeterministicFactor("m_t", mtF)); //total mass (after collision)
        bn.addFactor(new DeterministicFactor("v_t", vtF)); //total velocity (after collision)
        return bn;


//        SamplerInterface sampler = handler.makeSampler(bn, ("v_1 v_" + (n-1)).split(" "), //todo what about this?
//                evidence, minVarLimit, maxVarLimit, jointToSampler
//                FractionalJointBaselineGibbsSampler.makeJointToSampler()
//                FractionalJointRejectionSampler.makeJointToSampler(1)
//                SelfTunedFractionalJointMetropolisHastingSampler.makeJointToSampler(10, 30, 100)
//                FractionalJointMetropolisHastingSampler.makeJointToSampler(10)
//                SymbolicFractionalJointGibbsSampler.makeJointToSampler()
//        );

    }

    private class JointWrapper {
        PiecewiseExpression<Fraction> joint;
        double minVarLimit;
        double maxVarLimit;

        private JointWrapper(PiecewiseExpression<Fraction> joint, double minVarLimit, double maxVarLimit) {
            this.joint = joint;
            this.minVarLimit = minVarLimit;
            this.maxVarLimit = maxVarLimit;
        }
    }

    private interface Param2JointWrapper {
        JointWrapper makeJointWrapper(int param);
    }
}

