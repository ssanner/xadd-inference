package hgm.poly.reports;

import hgm.BayesianDataGenerator;
import hgm.asve.Pair;
import hgm.poly.bayesian.PriorHandler;
import hgm.poly.market.DummyMarketMakingDatabase;
import hgm.poly.pref.BayesianPairwisePreferenceLearningModel;
import hgm.poly.sampling.SamplerInterface;
import hgm.preference.db.DummyFeasiblePreferenceDatabase;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 10/05/14
 * Time: 8:58 PM
 */
public class LinearTimeGibbsSamplingInPiecewiseGMsTester {

    public static final String REPORT_PATH_FOR_FINAL_BPPL_DIM_ANALYSIS = ".//REPORT_PATH/BPPL-DIM/1/";
    public static final String REPORT_PATH_FOR_FINAL_BPPL_DATA_ANALYSIS = "./REPORT_PATH3/BPPL-DATA/1/";
    public static final String REPORT_PATH_FOR_FINAL_MMM_DIM_ANALYSIS = "./REPORT_PATH3/MMM-DIM/1/";
    public static final String REPORT_PATH_FOR_FINAL_MMM_DATA_ANALYSIS = "./REPORT_PATH3/MMM-DATA/1/";

    enum Test{BPPL_DIM_TEST, BPPL_DATA_TEST, MMM_DIM_TEST, MMM_DATA_TEST}

    public static void main(String[] args) throws IOException {

        Test test = Test.BPPL_DIM_TEST;

        LinearTimeGibbsSamplingInPiecewiseGMsTester instance = new LinearTimeGibbsSamplingInPiecewiseGMsTester();
        switch (test) {
            case BPPL_DATA_TEST:
                instance.bpplDataAnalysisTest();
                break;
            case BPPL_DIM_TEST:
                instance.bpplDimAnalysisTest();
                break;
            case MMM_DATA_TEST:
                instance.mmmDataAnalysisTest();
                break;
            case MMM_DIM_TEST:
                instance.mmmDimAnalysisTest();
                break;
            default:
                throw new RuntimeException("unknown test...");
        }
    }

    interface ParamDataCount2DataGenerator {

        BayesianDataGenerator createDataGenerator(int paramSpaceDim, int numObservedDataPoints);

    }

    public static final int FIXED_DATA_FOR_BPPL_FINAL_DIM_ANALYSIS = 12;
    public static final int FIXED_DIM_FOR_FINAL_BPPL_DATA_ANALYSIS = 15;

    public static final String TESTER_ALGORITHM_FOR_FINAL_BPPL_TESTS = SamplingAlgorithmBank.REJ_ORIGINAL_MODEL_BPPL;

    public static final String[] TESTED_ALGORITHMS_FOR_FINAL_BPPL_TESTS = new String[]{
            SamplingAlgorithmBank.TARGETED_GATED_GIBBS_GENERAL_BPPL
            , SamplingAlgorithmBank.TUNED_MH_GENERAL_BPPL
            , SamplingAlgorithmBank.REJ_GENERAL_BPPL
            , SamplingAlgorithmBank.FULL_GIBBS_GENERAL_BPPL
    };
    //
    public static final int FIXED_DATA_FOR_MMM_DIM_ANALYSIS = 8;
    public static final int FIXED_DIM_FOR_MMM_DATA_ANALYSIS = 10;

    public static final int NUM_ALG_ITERS_FOR_MMM_DIM_ANALYSIS = 3; //todo

    public static final String[] TESTED_ALGORITHMS_FOR_FINAL_MMM_TESTS = new String[]{
            SamplingAlgorithmBank.TUNED_MH_GENERAL_MMM
            , SamplingAlgorithmBank.REJ_GENERAL_MMM
            , SamplingAlgorithmBank.TARGETED_GATED_GIBBS_GENERAL_MMM
            , SamplingAlgorithmBank.FULL_GIBBS_GENERAL_MMM
    };


    public void bpplDimAnalysisTest() throws IOException {
        System.out.println("REPORT_PATH_FOR_FINAL_BPPL_DIM_ANALYSIS = " + REPORT_PATH_FOR_FINAL_BPPL_DIM_ANALYSIS);
        //BPPL params:
        final double bppl_indicatorNoise = 0.4;
        final int maxGatingConditionViolation = Integer.MAX_VALUE;
        final int numberOfItems = 100; // Does not have any significant effect (unless if its too small, dummy items will be repeated)...
        final double minAttribBound = 0d;
        final double maxAttribBound = 5d;

        List<Db2Sampler> algorithmsBeingTested = null;
        ParamDataCount2DataGenerator paramDataCount2dataGenerator = null;
        Db2Sampler testerAlgorithm = null;

        testerAlgorithm = SamplingAlgorithmBank.makeDb2Samplers4PrefLearningModel(bppl_indicatorNoise, maxGatingConditionViolation,
                TESTER_ALGORITHM_FOR_FINAL_BPPL_TESTS).get(0);

        algorithmsBeingTested = SamplingAlgorithmBank.makeDb2Samplers4PrefLearningModel(bppl_indicatorNoise, maxGatingConditionViolation,
                TESTED_ALGORITHMS_FOR_FINAL_BPPL_TESTS);

        paramDataCount2dataGenerator = new ParamDataCount2DataGenerator() {
            @Override
            public BayesianDataGenerator createDataGenerator(int numDims, int numObservedDataPoints) {
                PriorHandler prior = PriorHandler.uniformInHypercube("w", numDims, BayesianPairwisePreferenceLearningModel.C);
                return new DummyFeasiblePreferenceDatabase(
                        minAttribBound,
                        maxAttribBound,
                        numObservedDataPoints,
                        prior,
                        numberOfItems /* number of items */,
                        bppl_indicatorNoise);
            }
        };


        //Testing params:
        int[] numDimsArray = new int[]{3, 4, 5, 6, 7, 8, 10, 12, 14, 16, 18, 20, 22, 25, 26, 28, 30, 35, 40, 45, 50, 60, 80};
        int[] numObservedDataPointsArray = new int[]{FIXED_DATA_FOR_BPPL_FINAL_DIM_ANALYSIS};//{2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 34, 38, 42, 48, 55, 65, 70, 80, 90, 100};//{3, 4, 5, 7, 9, 12, 15, 18, 25, 35};//{5, 7, 9, 12, 14, 18, 22, 25}; //num. observed data
        Integer numSamplesFromTesterToSimulateTrueDistribution = 1000000;//550000;//for BPPL/dim10;constr10: 50000;
        int numMinDesiredSamples = 3;//10000;//10000;
        int numIterationsForEachAlgorithm = 3;//10
        int burnedSamples = 0;

        long maxWaitingTimeForTesterToSimulateTrueDistributionMillis = (int) 4*60*1000;//(15 * 60 * 1000); //todo!!!!!!!!!!!!
        long maxWaitingTimeForTakingDesiredSamplesMillis = 1*60*1000;//(10 * 60 * 1000);//10 * 60 * 1000;
        long minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis = maxWaitingTimeForTakingDesiredSamplesMillis;//(int) (1 * 60 * 1000);//10*60*1000; //(int) (10* 60 * 1000); //****

        Integer numTimePointsForWhichErrIsPersisted = 500;

        double goldenErrThreshold = 3d; //Another terminating condition: If reached sampling would be terminated. If you do not like it make it 0. or negative

        testSamplersPerformanceWrtDimsAndConstraints(
                paramDataCount2dataGenerator,
                testerAlgorithm,
                numSamplesFromTesterToSimulateTrueDistribution,
                maxWaitingTimeForTesterToSimulateTrueDistributionMillis,
                algorithmsBeingTested,
                numDimsArray,
                numObservedDataPointsArray,
                numMinDesiredSamples,
                maxWaitingTimeForTakingDesiredSamplesMillis,
                minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
                numTimePointsForWhichErrIsPersisted,
                numIterationsForEachAlgorithm,
                burnedSamples,
                REPORT_PATH_FOR_FINAL_BPPL_DIM_ANALYSIS,
                goldenErrThreshold);

        //*****************************************************************************************

        System.out.println("TESTER: that was all folk FOR BPPL Dim analysis");
    }

    public void bpplDataAnalysisTest() throws IOException {
        System.out.println("REPORT_PATH_FOR_FINAL_BPPL_DATA_ANALYSIS = " + REPORT_PATH_FOR_FINAL_BPPL_DATA_ANALYSIS);
        //BPPL params:
        final double bppl_indicatorNoise = 0.4;
        final int maxGatingConditionViolation = Integer.MAX_VALUE;
        final int numberOfItems = 100; // shouldn't have any significant effect (?) unless if its too small, dummy items will be repeated...
        final double minAttribBound = 0d;
        final double maxAttribBound = 5d;

        List<Db2Sampler> algorithmsBeingTested = null;
        ParamDataCount2DataGenerator paramDataCount2dataGenerator = null;
        Db2Sampler testerAlgorithm = null;

        testerAlgorithm = SamplingAlgorithmBank.makeDb2Samplers4PrefLearningModel(bppl_indicatorNoise, maxGatingConditionViolation,
                TESTER_ALGORITHM_FOR_FINAL_BPPL_TESTS).get(0);

        algorithmsBeingTested = SamplingAlgorithmBank.makeDb2Samplers4PrefLearningModel(bppl_indicatorNoise, maxGatingConditionViolation,
                TESTED_ALGORITHMS_FOR_FINAL_BPPL_TESTS);

        paramDataCount2dataGenerator = new ParamDataCount2DataGenerator() {
            @Override
            public BayesianDataGenerator createDataGenerator(int numDims, int numObservedDataPoints) {
                PriorHandler prior = PriorHandler.uniformInHypercube("w", numDims, BayesianPairwisePreferenceLearningModel.C);
                return new DummyFeasiblePreferenceDatabase(
                        minAttribBound,
                        maxAttribBound,
                        numObservedDataPoints,
                        prior,
                        numberOfItems /* number of items */,
                        bppl_indicatorNoise);
            }
        };


        //Testing params:
        int[] numDimsArray = new int[]{FIXED_DIM_FOR_FINAL_BPPL_DATA_ANALYSIS};//{3, 4, 5, 6, 7, 8, 10, 12, 14, 16, 18, 20, 22, 25, 26, 28, 30, 35, 40, 45, 50, 60, 80};
        int[] numObservedDataPointsArray = new int[]{2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18};//, 20, 22, 24, 26, 28, 30, 34, 38, 42, 48, 55, 65, 70, 80, 90, 100};//{3, 4, 5, 7, 9, 12, 15, 18, 25, 35};//{5, 7, 9, 12, 14, 18, 22, 25}; //num. observed data
        Integer numSamplesFromTesterToSimulateTrueDistribution = 1000000;//550000;//for BPPL/dim10;constr10: 50000;
        int numMinDesiredSamples = 3;//10000;//10000;
        int numIterationsForEachAlgorithm = 3;//10
        int burnedSamples = 0;

        long maxWaitingTimeForTesterToSimulateTrueDistributionMillis = (int) (5 * 60 * 1000);//(15*60 * 1000); //todo!!!!!!!!!!!!
        long maxWaitingTimeForTakingDesiredSamplesMillis = (1 * 60 * 1000);//10 * 60 * 1000;
        long minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis = maxWaitingTimeForTakingDesiredSamplesMillis;//(int) (1 * 60 * 1000);//10*60*1000; //(int) (10* 60 * 1000); //****

        Integer numTimePointsForWhichErrIsPersisted = 500;

        double goldenErrThreshold = 3d; //Another terminating condition: If reached sampling would be terminated. If you do not like it make it 0. or negative

        testSamplersPerformanceWrtDimsAndConstraints(
                paramDataCount2dataGenerator,
                testerAlgorithm,
                numSamplesFromTesterToSimulateTrueDistribution,
                maxWaitingTimeForTesterToSimulateTrueDistributionMillis,
                algorithmsBeingTested,
                numDimsArray,
                numObservedDataPointsArray,
                numMinDesiredSamples,
                maxWaitingTimeForTakingDesiredSamplesMillis,
                minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
                numTimePointsForWhichErrIsPersisted,
                numIterationsForEachAlgorithm,
                burnedSamples,
                REPORT_PATH_FOR_FINAL_BPPL_DATA_ANALYSIS,
                goldenErrThreshold);

        //*****************************************************************************************

        System.out.println("TESTER: that was all folk FOR BPPL Dim analysis");
    }



    public void mmmDataAnalysisTest() throws IOException {
        System.out.println("REPORT_PATH_FOR_FINAL_MMM_DATA_ANALYSIS = " + REPORT_PATH_FOR_FINAL_MMM_DATA_ANALYSIS);

        //MM params:
        final double mm_epsilon_for_star_vars = BayesianPairwisePreferenceLearningModel.C / 4.0;

        List<Db2Sampler> algorithmsBeingTested;
        ParamDataCount2DataGenerator paramDataCount2dataGenerator;
        Db2Sampler testerAlgorithm;

        testerAlgorithm = SamplingAlgorithmBank.makeDb2Samplers4MarketMakingModel(
                SamplingAlgorithmBank.REJ_GENERAL_MMM).get(0);

        algorithmsBeingTested = SamplingAlgorithmBank.makeDb2Samplers4MarketMakingModel(TESTED_ALGORITHMS_FOR_FINAL_MMM_TESTS);

        paramDataCount2dataGenerator = new ParamDataCount2DataGenerator() {
            @Override
            public BayesianDataGenerator createDataGenerator(int numDims, int numObservedDataPoints) {
                PriorHandler prior =
                        PriorHandler.uniformInHypercube("v", numDims, BayesianPairwisePreferenceLearningModel.C);

                return new DummyMarketMakingDatabase(
                        numObservedDataPoints,
                        prior,
                        mm_epsilon_for_star_vars //for v*
                );
            }
        };


        //Testing params:
        int[] numDimsArray = new int[]{LinearTimeGibbsSamplingInPiecewiseGMsTester.FIXED_DIM_FOR_MMM_DATA_ANALYSIS};//{3, 4, 5, 6, 7, 8, 10, 12, 14, 16, 18, 20, 22, 25, 26, 28, 30, 35, 40, 45, 50, 60, 80};
        int[] numObservedDataPointsArray = new int[]{2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 34, 38, 42, 48, 55, 65, 70, 80, 90, 100};//{3, 4, 5, 7, 9, 12, 15, 18, 25, 35};//{5, 7, 9, 12, 14, 18, 22, 25}; //num. observed data
        Integer numSamplesFromTesterToSimulateTrueDistribution = 1000000;//550000;//for BPPL/dim10;constr10: 50000;
        int numMinDesiredSamples = 3;//10000;//10000;
        int numIterationsForEachAlgorithm = NUM_ALG_ITERS_FOR_MMM_DIM_ANALYSIS;//10
        int burnedSamples = 0;

        long maxWaitingTimeForTesterToSimulateTrueDistributionMillis = (int) (30 * 60 * 1000);//was 2...(15*60 * 1000); //todo!!!!!!!!!!!!
        long maxWaitingTimeForTakingDesiredSamplesMillis = (5 * 60 * 1000);//10 * 60 * 1000;
        long minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis = maxWaitingTimeForTakingDesiredSamplesMillis;//(int) (1 * 60 * 1000);//10*60*1000; //(int) (10* 60 * 1000); //****

        Integer numTimePointsForWhichErrIsPersisted = 500;

        double goldenErrThreshold = 3d; //Another terminating condition: If reached sampling would be terminated. If you do not like it make it 0. or negative

        testSamplersPerformanceWrtDimsAndConstraints(
                paramDataCount2dataGenerator,
                testerAlgorithm,
                numSamplesFromTesterToSimulateTrueDistribution,
                maxWaitingTimeForTesterToSimulateTrueDistributionMillis,
                algorithmsBeingTested,
                numDimsArray,
                numObservedDataPointsArray,
                numMinDesiredSamples,
                maxWaitingTimeForTakingDesiredSamplesMillis,
                minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
                numTimePointsForWhichErrIsPersisted,
                numIterationsForEachAlgorithm,
                burnedSamples,
                REPORT_PATH_FOR_FINAL_MMM_DATA_ANALYSIS,
                goldenErrThreshold);

        //*****************************************************************************************

        System.out.println("TESTER: that was all folk");

    }



    public void mmmDimAnalysisTest() throws IOException {
        System.out.println("REPORT_PATH_FOR_FINAL_MMM_DIM_ANALYSIS = " + REPORT_PATH_FOR_FINAL_MMM_DIM_ANALYSIS);

        //MM params:
        final double mm_epsilon_for_star_vars = BayesianPairwisePreferenceLearningModel.C / 4.0;

        //----------------------------------------

        List<Db2Sampler> algorithmsBeingTested;
        ParamDataCount2DataGenerator paramDataCount2dataGenerator;
        Db2Sampler testerAlgorithm;

        testerAlgorithm = SamplingAlgorithmBank.makeDb2Samplers4MarketMakingModel(
                        SamplingAlgorithmBank.REJ_GENERAL_MMM).get(0);

        algorithmsBeingTested = SamplingAlgorithmBank.makeDb2Samplers4MarketMakingModel(TESTED_ALGORITHMS_FOR_FINAL_MMM_TESTS);

        paramDataCount2dataGenerator = new ParamDataCount2DataGenerator() {
            @Override
            public BayesianDataGenerator createDataGenerator(int numDims, int numObservedDataPoints) {
                PriorHandler prior =
                                PriorHandler.uniformInHypercube("v", numDims, BayesianPairwisePreferenceLearningModel.C);

                return new DummyMarketMakingDatabase(
                        numObservedDataPoints,
                        prior,
                        mm_epsilon_for_star_vars //for v*
                );
            }
        };

        //Testing params:
        int[] numDimsArray = new int[]{3, 4, 5, 6, 7, 8, 10, 12, 14, 16, 18, 20, 22, 25, 26, 28, 30, 35, 40, 45, 50};//, 60, 80};
        int[] numObservedDataPointsArray = new int[]{LinearTimeGibbsSamplingInPiecewiseGMsTester.FIXED_DATA_FOR_MMM_DIM_ANALYSIS};//{2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 34, 38, 42, 48, 55, 65, 70, 80, 90, 100};//{3, 4, 5, 7, 9, 12, 15, 18, 25, 35};//{5, 7, 9, 12, 14, 18, 22, 25}; //num. observed data
        Integer numSamplesFromTesterToSimulateTrueDistribution = 1000000;//550000;//for BPPL/dim10;constr10: 50000;
        int numMinDesiredSamples = 3;//10000;//10000;
        int numIterationsForEachAlgorithm = NUM_ALG_ITERS_FOR_MMM_DIM_ANALYSIS;//10
        int burnedSamples = 0;

        long maxWaitingTimeForTesterToSimulateTrueDistributionMillis = (int) (5 * 60 * 1000);//2(15*60 * 1000); //todo!!!!!!!!!!!!
        long maxWaitingTimeForTakingDesiredSamplesMillis = (1 * 60 * 1000);//10 * 60 * 1000;
        long minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis = maxWaitingTimeForTakingDesiredSamplesMillis;//(int) (1 * 60 * 1000);//10*60*1000; //(int) (10* 60 * 1000); //****

        Integer numTimePointsForWhichErrIsPersisted = 500;

        double goldenErrThreshold = 3d; //Another terminating condition: If reached sampling would be terminated. If you do not like it make it 0. or negative

        testSamplersPerformanceWrtDimsAndConstraints(
                paramDataCount2dataGenerator,
                testerAlgorithm,
                numSamplesFromTesterToSimulateTrueDistribution,
                maxWaitingTimeForTesterToSimulateTrueDistributionMillis,
                algorithmsBeingTested,
                numDimsArray,
                numObservedDataPointsArray,
                numMinDesiredSamples,
                maxWaitingTimeForTakingDesiredSamplesMillis,
                minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
                numTimePointsForWhichErrIsPersisted,
                numIterationsForEachAlgorithm,
                burnedSamples,
                REPORT_PATH_FOR_FINAL_MMM_DIM_ANALYSIS,
                goldenErrThreshold);

        //*****************************************************************************************

        System.out.println("TESTER: that was all folk");

    }



    private void testSamplersPerformanceWrtDimsAndConstraints(
            ParamDataCount2DataGenerator paramDataCount2DataGenerator,
            Db2Sampler testerSampleMaker,  //used as baseline
            int numSamplesFromTesterToSimulateTrueDistribution,
            long maxWaitingTimeForTesterToSimulateMillis,
            List<Db2Sampler> samplerMakersToBeTested,
            int[] numDimsArray, // parameter space dimension
            int[] numObservedDataPointsArray,
            int numMinDesiredSamples,     //used for error vs. #samples diagram
            long maxWaitingTimeForTakingDesiredSamples, //if sampling takes more than this, sampling would be terminated without an answer
            long minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis, //used for error vs. time diagram
            int approxNumTimePointsForWhichErrIsPersisted,
            int numRuns,
            int burnedSamples, //used for both tester and tested algorithms
            String outputDirectoryPath,
            double goldenErrThreshold) throws IOException {

        TotalTimeKeeper timeKeeper = new TotalTimeKeeper(samplerMakersToBeTested, numDimsArray, numObservedDataPointsArray, outputDirectoryPath);

        AlgorithmDeathLimitKeeper testedAlgsDeathKeeper = new AlgorithmDeathLimitKeeper(samplerMakersToBeTested);


        //note: both arrays should be sorted.
        for (int numDims : numDimsArray) {
            for (int numObservedDataPoints : numObservedDataPointsArray) {    //num constraints

                System.out.println(".......\nnumDims = " + numDims);
                System.out.println("numObservedDataPoints = " + numObservedDataPoints);

                BayesianDataGenerator db = paramDataCount2DataGenerator.createDataGenerator(numDims, numObservedDataPoints);

                double[] groundTruthMeans;
                try {
                    groundTruthMeans = computeGroundTruthMean(db, testerSampleMaker, numSamplesFromTesterToSimulateTrueDistribution, maxWaitingTimeForTesterToSimulateMillis);
                    System.out.println("{{{groundTruthMeans = " + Arrays.toString(groundTruthMeans) + "}}}");
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("NO GROUND TRUTH TAKEN! TERMINATED....");
                    continue;
                }

                //Analysis of tested algorithms:
                for (Db2Sampler samplerMaker : samplerMakersToBeTested) {
                    if (!testedAlgsDeathKeeper.algorithmWillDie(samplerMaker, numDims, numObservedDataPoints)) {
                        StatInfo statInfo = meansAndStdErrors(numRuns,
                                groundTruthMeans,
                                db,
                                samplerMaker,
                                burnedSamples,
                                numMinDesiredSamples,
                                maxWaitingTimeForTakingDesiredSamples,
                                minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
                                approxNumTimePointsForWhichErrIsPersisted,
                                goldenErrThreshold);
                        statInfo.persistMeanStdErrForFirstTakenSamples(outputDirectoryPath, numDims, numObservedDataPoints, samplerMaker.getName(), numMinDesiredSamples);
                        statInfo.persistMeanStdErrForTimePoints(outputDirectoryPath, numDims, numObservedDataPoints, samplerMaker.getName(), maxWaitingTimeForTakingDesiredSamples);

                        if (statInfo.timeToTakeFirstSamplesOrGoldenTime != null) { //did not die
                            timeKeeper.persist(numDims, numObservedDataPoints, samplerMaker.getName(), statInfo.timeToTakeFirstSamplesOrGoldenTime);
                        } else { //died
                            testedAlgsDeathKeeper.recordDeath(samplerMaker, numDims, numObservedDataPoints);
                        }

                        System.out.println(samplerMaker.getName() + ".timeN/GOLD = " + statInfo.timeToTakeFirstSamplesOrGoldenTime/*totalProcessTimeMillis*/ + "\t\tsamples=" + statInfo.means4FirstSamples.size());

                    } else {
                        System.err.println(samplerMaker.getName() + " skipped...");
                    }
                }
            }
        }

    }

    public double[] computeGroundTruthMean(
            BayesianDataGenerator db, Db2Sampler samplerMaker,
            final int numDesiredSamples,
            long maxWaitingTimeForTakingDesiredSamplesMillis //in ms
    ) {
        double[] mean = null;
        int numTakenSamples = 0;
        SamplerInterface sampler = samplerMaker.createSampler(db);
//        List<Double[]> groundTruthSamples = new ArrayList<Double[]>(numDesiredSamples); //this will be saved...
        long t1 = System.currentTimeMillis();
        for (int sampleCount = 0; sampleCount < numDesiredSamples; sampleCount++) {
            Double[] sample = sampler.reusableSample();

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

    public StatInfo meansAndStdErrors(
            int numRunsPerAlgorithm,
            double[] groundTruthMeanVector, //of size #dims
            BayesianDataGenerator db,
            Db2Sampler samplerMaker,//SamplerInterface sampler,
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

            Err4Samples_Err4times_Times err4Samples_err4times_times = errorVsSamplesAndTime(groundTruthMeanVector, db, samplerMaker, burnedSamples,
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

        Long averageTimeToAccomplishOrGolden = null;
        if (!timesToAccomplishMillisOrGoldenNanos.isEmpty()) {
            averageTimeToAccomplishOrGolden = 0L;
            for (Long time : timesToAccomplishMillisOrGoldenNanos) {
                averageTimeToAccomplishOrGolden += time;
            }
            averageTimeToAccomplishOrGolden /= timesToAccomplishMillisOrGoldenNanos.size();
        }

        return new StatInfo(Arrays.asList(meanFirstSampledErrs),
                Arrays.asList(stdErrs4FirstSamples),
                meanErrForTimes, Arrays.asList(stdErrs4Times), hallmarkTimeStampsInNano, averageTimeToAccomplishOrGolden, numRunsPerAlgorithm);

    }

    public Err4Samples_Err4times_Times errorVsSamplesAndTime(
            double[] groundTruthMeanVector, //of size #dims
            BayesianDataGenerator db,
            Db2Sampler samplerMaker,
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
        SamplerInterface sampler = samplerMaker.createSampler(db);
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
                System.out.println("sampling successfully terminated due to reaching golden error rate: " + goldenErrRecorded + " -in " + goldenErrTimeMillisOrNano + "(ns) after taking " + takenSamples + " samples");
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
        return new Err4Samples_Err4times_Times(errVsFirstSamples, errVsTimes, recordedTimePointsInNano,
                samplingPerformedInIntendedTimeSuccessfully, goldenErrTimeMillisOrNano/*timeToAccomplishTaskMillis*/);
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

        private String generalInfo(int numParamDims, int numDataPoints, String algorithmName/*, int maxAllowedSamplingTime*/) {
            return "dim" + numParamDims + "-data" + numDataPoints + "-itr" + numIterationsForEachAlgorithm /*+ "-maxT" + maxAllowedSamplingTime */ + "-" + algorithmName;
        }

        public void persistMeanStdErrForFirstTakenSamples(String path, int numParamDims, int numDataPoints, String algorithmName, int numDesiredSamples) throws FileNotFoundException {
            PrintStream ps;
            String outputFileName = path + generalInfo(numParamDims, numDataPoints, algorithmName/*, maxAllowedSamplingTime*/) + "-samples" + numDesiredSamples;

            ps = new PrintStream(new FileOutputStream(outputFileName));

            for (int i = 0; i < means4FirstSamples.size(); i++) {
                //          #sample         #mean       #stdErr
                ps.println((i + 1) + "\t" + means4FirstSamples.get(i) + "\t" + stdErrs4FirstSamples.get(i));
            }

            ps.close();
        }


        public void persistMeanStdErrForTimePoints(String path, int numParamDims, int numDataPoints, String algorithmName, long maxAllowedSamplingTime) throws FileNotFoundException {
            String outputFileName = path + generalInfo(numParamDims, numDataPoints, algorithmName/*, maxAllowedSamplingTime*/) + "-times";// + means4TimePoints.size();

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

        public TotalTimeKeeper(Db2Sampler[] samplers, String outputDirectoryPath) {
            this.outputDirectoryPath = outputDirectoryPath;

            alg2dataTime = new HashMap<String, SortedSet<Pair<Integer, Long>>>(samplers.length);
            alg2dimsTime = new HashMap<String, SortedSet<Pair<Integer, Long>>>(samplers.length);

            for (Db2Sampler sampler : samplers) {
                alg2dataTime.put(sampler.getName(), new TreeSet<Pair<Integer, Long>>());
            }
        }

        int[] dimsArray;
        int[] dataArray;
        String[] samplerNames;
        Map<String /*alg*/, Long/*time*/>[][] dimIndexDataIndexAlgTime;

        public TotalTimeKeeper(List<Db2Sampler> samplers, int[] dimsArray, int[] dataArray, String outputDirectoryPath) {
            this.outputDirectoryPath = outputDirectoryPath;

            this.dimsArray = new int[dimsArray.length];
            this.dataArray = new int[dataArray.length];
            System.arraycopy(dimsArray, 0, this.dimsArray, 0, dimsArray.length);
            System.arraycopy(dataArray, 0, this.dataArray, 0, dataArray.length);
            Arrays.sort(this.dimsArray);
            Arrays.sort(this.dataArray);

            dimIndexDataIndexAlgTime = new HashMap[dimsArray.length][dataArray.length];

            this.samplerNames = new String[samplers.size()];
            for (int i = 0; i < samplers.size(); i++) {
                this.samplerNames[i] = samplers.get(i).getName();
            }

            for (int i = 0; i < dimsArray.length; i++) {
                for (int j = 0; j < dataArray.length; j++) {
                    dimIndexDataIndexAlgTime[i][j] = new HashMap<String, Long>(samplers.size());
                }
            }
        }

        public void persist(int dim, int data, String samplerName, Long timeMillis) throws FileNotFoundException {
            int dimIndex = Arrays.binarySearch(dimsArray, dim);
            int dataIndex = Arrays.binarySearch(dataArray, data);
            if (dimIndexDataIndexAlgTime[dimIndex][dataIndex].put(samplerName, timeMillis) != null) {
                System.err.println("for dim: " + dim + " /data: " + data + ", " + samplerName + " already exists! and will be replaced");
                System.out.println("dimsArray = " + Arrays.toString(dimsArray));
                System.out.println("dataArray = " + Arrays.toString(dataArray));
            }
            persistDimFix(samplerName, dimIndex);
            persistDataFix(samplerName, dataIndex);
        }

        private void persistDimFix(String samplerName, int dimIndex) throws FileNotFoundException {
            int dim = dimsArray[dimIndex];

            List<Integer> dataList = new ArrayList<Integer>();
            List<Long> timeList = new ArrayList<Long>();
            for (int dataIndex = 0; dataIndex < dataArray.length; dataIndex++) {
                Long time = dimIndexDataIndexAlgTime[dimIndex][dataIndex].get(samplerName);
                if (time == null) continue;
                int data = dataArray[dataIndex];
                dataList.add(data);
                timeList.add(time);
            }
            persistOneEntryFixed(samplerName, "dim", dim, dataList, timeList);
        }

        private void persistDataFix(String samplerName, int dataIndex) throws FileNotFoundException {
            int data = dataArray[dataIndex];

            List<Integer> dimList = new ArrayList<Integer>();
            List<Long> timeList = new ArrayList<Long>();
            for (int dimIndex = 0; dimIndex < dimsArray.length; dimIndex++) {
                Long time = dimIndexDataIndexAlgTime[dimIndex][dataIndex].get(samplerName);
                if (time == null) continue;
                int dim = dimsArray[dimIndex];
                dimList.add(dim);
                timeList.add(time);
            }
            persistOneEntryFixed(samplerName, "data", data, dimList, timeList);
        }

        private void persistOneEntryFixed(String samplerName, String fixedElement, int fixedElementValue, List<Integer> otherElementList, List<Long> timeList) throws FileNotFoundException {
            String outputFileName = this.outputDirectoryPath + "fixed-" + fixedElement + fixedElementValue + "-" + samplerName;

            PrintStream ps = new PrintStream(new FileOutputStream(outputFileName));

            int n = otherElementList.size();
            if (n != timeList.size()) throw new RuntimeException("size mismatch");
            for (int i = 0; i < n; i++) {
                ps.println(otherElementList.get(i) + "\t" + timeList.get(i));
            }

            ps.close();
        }

    }
}


