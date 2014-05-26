package hgm.poly.reports;

import hgm.BayesianDataGenerator;
import hgm.asve.Pair;
import hgm.poly.bayesian.PriorHandler;
import hgm.poly.market.DummyMarketMakingDatabase;
import hgm.poly.pref.BayesianPairwisePreferenceLearningModel;
import hgm.poly.sampling.SamplerInterface;
import hgm.preference.db.DummyFeasiblePreferenceDatabase;

import java.io.*;
import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 10/05/14
 * Time: 8:58 PM
 */
public class Tester {
    public static final String REPORT_PATH = "E:/REPORT_PATH/";

    public static void main(String[] args) throws IOException {
        Tester instance = new Tester();
        instance.nipsTest();
    }

    interface ParamDataCount2DataGenerator {
        BayesianDataGenerator createDataGenerator(int paramSpaceDim, int numObservedDataPoints);
    }

    public void nipsTest() throws IOException {

        //BPPL params:
        final double bppl_indicatorNoise = 0.4;
        final int maxGatingConditionViolation = Integer.MAX_VALUE;
        final int numberOfItems = 100; // shouldn't have any significant effect (?) unless if its too small, dummy items will be repeated...
        final double minAttribBound = 0d;
        final double maxAttribBound = 5d;

        //MM params:
        final double mm_epsilon_for_star_vars = BayesianPairwisePreferenceLearningModel.C / 10.0;

        //----------------------------------------

        ModelType problem =
                ModelType.MMM;
//                ModelType.BPPL;

        Db2Sampler[] algorithmsBeingTested = null;
        ParamDataCount2DataGenerator paramDataCount2dataGenerator = null;
        Db2Sampler testerAlgorithm = null;

        switch (problem) {
            case MMM:
                testerAlgorithm = SamplingAlgorithmBank.makeDb2Samplers4MarketMakingModel(
                        SamplingAlgorithmBank.REJ_GENERAL_MMM)[0];
//                        SamplingAlgorithmBank.REJ_ORIGINAL_MODEL_MMM)[0];
//                        SamplingAlgorithmBank.TARGETED_GATED_GIBBS_GENERAL_MMM)[0];


                algorithmsBeingTested = SamplingAlgorithmBank.makeDb2Samplers4MarketMakingModel(
                        SamplingAlgorithmBank.TARGETED_GATED_GIBBS_GENERAL_MMM,
                        SamplingAlgorithmBank.MH_GENERAL_MMM,
                        SamplingAlgorithmBank.GATED_GIBBS_GENERAL_MMM,
                        SamplingAlgorithmBank.REJ_ORIGINAL_MODEL_MMM,
                        SamplingAlgorithmBank.REJ_GENERAL_MMM,
                        SamplingAlgorithmBank.FULL_GIBBS_GENERAL_MMM
                );

                paramDataCount2dataGenerator = new ParamDataCount2DataGenerator() {
                    @Override
                    public BayesianDataGenerator createDataGenerator(int numDims, int numObservedDataPoints) {
                        PriorHandler prior =
                                //todo: change to serial dependent...
//                                PriorHandler.serialDependent("v",
//                                        numDims, BayesianPairwisePreferenceLearningModel.C/*first uniform bound*/,
//                                        BayesianPairwisePreferenceLearningModel.C / 5.0/*conditional dependence parameter*/);
                                PriorHandler.uniformInHypercube("v", numDims, BayesianPairwisePreferenceLearningModel.C);

                        return new DummyMarketMakingDatabase(
                                numObservedDataPoints,
                                prior,
                                mm_epsilon_for_star_vars //for v*
                        );
                    }
                };

                break;

            case BPPL:
                testerAlgorithm = SamplingAlgorithmBank.makeDb2Samplers4PrefLearningModel(bppl_indicatorNoise, maxGatingConditionViolation,
                        SamplingAlgorithmBank.REJ_GENERAL_BPPL)[0];
//                        SamplingAlgorithmBank.REJ_ORIGINAL_MODEL_BPPL)[0];
//                        SamplingAlgorithmBank.TARGETED_GATED_GIBBS_CONST_BPPL)[0];

                algorithmsBeingTested = SamplingAlgorithmBank.makeDb2Samplers4PrefLearningModel(bppl_indicatorNoise, maxGatingConditionViolation
                        ,SamplingAlgorithmBank.REJ_GENERAL_BPPL
                        ,SamplingAlgorithmBank.MH_GENERAL_BPPL
//                        ,SamplingAlgorithmBank.SYMBOLIC_GIBBS_CONST_BPPL
                        ,SamplingAlgorithmBank.TARGETED_GATED_GIBBS_CONST_BPPL
                        ,SamplingAlgorithmBank.GATED_GIBBS_CONST_BPPL   //this is the best...
//                        ,SamplingAlgorithmBank.FULL_GIBBS_GENERAL_BPPL
//                        ,SamplingAlgorithmBank.REJ_ORIGINAL_MODEL_BPPL
//                        ,SamplingAlgorithmBank.MH_GENERAL_BPPL2
                        ,SamplingAlgorithmBank.GATED_GIBBS_GENERAL_BPPL
//                        ,SamplingAlgorithmBank.TESTER_CONST_BPPL
                        ,SamplingAlgorithmBank.FULL_GIBBS_CONST_BPPL
                );

                paramDataCount2dataGenerator = new ParamDataCount2DataGenerator() {
                    @Override
                    public BayesianDataGenerator createDataGenerator(int numDims, int numObservedDataPoints) {
                        PriorHandler prior = PriorHandler.uniformInHypercube("w", numDims, BayesianPairwisePreferenceLearningModel.C);
                        return new DummyFeasiblePreferenceDatabase(
//                                -GPolyPreferenceLearningModel.C,
//                                GPolyPreferenceLearningModel.C,
                                minAttribBound, //TODO:IMPORTANT check the effect of negative x_i bound...
                                maxAttribBound,
                                numObservedDataPoints, // + numberOfTestComparisonsPerDatabase /* more preferences used for testing */,
//                                numDims,
                                prior,
                                numberOfItems /* number of items */,
                                bppl_indicatorNoise);
                    }
                };

                break;
        }


        //Testing params:
        int[] numDimsArray = new int[]{10};//{3, 5, 8, 12, 16};
        int[] numObservedDataPointsArray = new int[]{10};//{2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20, 22, 25, 30, 35, 40, 50, 60, 70, 80, 90, 100};//{3, 4, 5, 7, 9, 12, 15, 18, 25, 35};//{5, 7, 9, 12, 14, 18, 22, 25}; //num. observed data
        Integer numSamplesFromTesterToSimulateTrueDistribution = 50000;//for BPPL/dim10;constr10: 50000;
        int numMinDesiredSamples = 10000;//10000;
        int numQueries = 20;
        int burnedSamples = 0;

        long maxWaitingTimeForTesterToSimulateTrueDistributionMillis = (int)(20 * 60 * 1000);
        long maxWaitingTimeForTakingDesiredSamplesMillis = (1*60*1000);//10 * 60 * 1000;
        long minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis = (int)(1*60*1000);//10*60*1000; //(int) (10* 60 * 1000); //****

        Integer numTimePointsForWhichErrIsPersisted = 500;

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
                numQueries,
                burnedSamples,
                REPORT_PATH);

        //*****************************************************************************************

        System.out.println("TESTER: that was all folk");

    }

    private void testSamplersPerformanceWrtDimsAndConstraints(
            ParamDataCount2DataGenerator paramDataCount2DataGenerator,
            Db2Sampler testerSampleMaker,  //used as baseline
            int numSamplesFromTesterToSimulateTrueDistribution,
            long maxWaitingTimeForTesterToSimulateMillis,
            Db2Sampler[] samplerMakersToBeTested,
            int[] numDimsArray, // parameter space dimension
            int[] numObservedDataPointsArray,
            int numMinDesiredSamples,     //used for error vs. #samples diagram
            long maxWaitingTimeForTakingDesiredSamples, //if sampling takes more than this, sampling would be terminated without an answer
            long minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis, //used for error vs. time diagram
            int approxNumTimePointsForWhichErrIsPersisted,
            int numQueries,
            int burnedSamples, //used for both tester and tested algorithms
            String outputDirectoryPath) throws IOException {

        //these are for generating qs:
        double minAttribBound = 0d;
        double maxAttribBound = 5d;


        TotalTimeKeeper timeKeeper = new TotalTimeKeeper(samplerMakersToBeTested, numDimsArray, numObservedDataPointsArray, outputDirectoryPath);

        AlgorithmDeathLimitKeeper testedAlgsDeathKeeper = new AlgorithmDeathLimitKeeper(samplerMakersToBeTested);
        AlgorithmDeathLimitKeeper testerDeathKeeper = new AlgorithmDeathLimitKeeper(new Db2Sampler[]{testerSampleMaker});


        //note: both arrays should be sorted.
        for (int numDims : numDimsArray) {
            for (int numObservedDataPoints : numObservedDataPointsArray) {    //num constraints

                System.out.println(".......\nnumDims = " + numDims);
                System.out.println("numObservedDataPoints = " + numObservedDataPoints);

                if (testerDeathKeeper.algorithmWillDie(testerSampleMaker, numDims, numObservedDataPoints)) {
                    System.err.println("It is believed that for #dims: " + numDims +
                            " #data: " + numObservedDataPoints +
                            " True distribution cannot be made. For this setting, whole Test abolished");
                    continue;
                }

                BayesianDataGenerator db = paramDataCount2DataGenerator.createDataGenerator(numDims, numObservedDataPoints);

                List<double[]> fs = createRandomItems(numQueries, numDims, minAttribBound, maxAttribBound);

                //Calc. ground truth means:
//                takeGroundSamples(db, testerSampleMaker, numSamplesFromTesterToSimulateTrueDistribution, maxWaitingTimeForTesterToSimulate);
                List<Double[]> groundTruth = fetchAndCompletePersistedGroundTruth(outputDirectoryPath, numDims, numObservedDataPoints, db, testerSampleMaker,
                        numSamplesFromTesterToSimulateTrueDistribution, maxWaitingTimeForTesterToSimulateMillis);

//                StatInfo groundTruthStat = meansAndStdErrors(fs, null /*groundTruthMeans is not calculated yet*/, db, testerSampleMaker,
//                        burnedSamples, numSamplesFromTesterToSimulateTrueDistribution/*numMinDesiredSamples*/,
//                        maxWaitingTimeForTesterToSimulate,
//                        0 /*no temporal analysis for baseline algorithm*/, 0 /*approxNumTimePointsForWhichErrIsPersisted not needed...*/);

                StatInfo groundTruthStat = meansAndStdErrors(fs, null /*groundTruthMeans is not calculated yet*/, db, testerSampleMaker,
                        burnedSamples, groundTruth.size()/*numSamplesFromTesterToSimulateTrueDistribution/*numMinDesiredSamples*/,
                        maxWaitingTimeForTesterToSimulateMillis,
                        0 /*no temporal analysis for baseline algorithm*/, 0 /*approxNumTimePointsForWhichErrIsPersisted not needed...*/,
                        groundTruth);
                if (groundTruthStat.timeToTakeFirstSamples/*.totalProcessTimeMillis*/ == null) {
                    System.err.println("Ground truth could not be made. Test with #Dims: " + numDims + " #Data: " + numObservedDataPoints + " is terminated...");
                    testerDeathKeeper.recordDeath(testerSampleMaker, numDims, numObservedDataPoints);
                    continue;
                }
                System.out.println("::groundTruthStat.timeToTakeFirstSamples = " + groundTruthStat.timeToTakeFirstSamples);

                double[] groundTruthMeans = groundTruthStat.groundTruthMeans;
                groundTruthStat = null; // I do not need other information, so I let garbage collector takes it.


                //Analysis of other algorithms:
                for (Db2Sampler samplerMaker : samplerMakersToBeTested) {
                    if (!testedAlgsDeathKeeper.algorithmWillDie(samplerMaker, numDims, numObservedDataPoints)) {
                    StatInfo statInfo = meansAndStdErrors(fs, groundTruthMeans, db, samplerMaker, burnedSamples,
                            numMinDesiredSamples, maxWaitingTimeForTakingDesiredSamples, minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis, approxNumTimePointsForWhichErrIsPersisted, null);
                    statInfo.persistMeanStdErrForFirstTakenSamples(outputDirectoryPath, numDims, numObservedDataPoints, samplerMaker.getName(), numMinDesiredSamples);
                    statInfo.persistMeanStdErrForTimePoints(outputDirectoryPath, numDims, numObservedDataPoints, samplerMaker.getName(), maxWaitingTimeForTakingDesiredSamples);

                    if (statInfo.timeToTakeFirstSamples != null) { //did not die
                        timeKeeper.persist(numDims, numObservedDataPoints, samplerMaker.getName(), statInfo.timeToTakeFirstSamples);
                    } else { //died
                        testedAlgsDeathKeeper.recordDeath(samplerMaker, numDims, numObservedDataPoints);
                    }

                    System.out.println(samplerMaker.getName() + ".timeN = " + statInfo.timeToTakeFirstSamples/*totalProcessTimeMillis*/ + "\t\tsamples=" + statInfo.means4FirstSamples.size());

                    } else {
                        System.err.println(samplerMaker.getName() + " skipped...");
                    }
                }
//                FunctionVisualizer.visualize(stdErr, 0, numSamples, 1, statType + " #dim:" + numDims + " #cnstrnt:" + numConstraints);
            }
        }

    }

    private List<Double[]> fetchAndCompletePersistedGroundTruth(String path,
                                                                int numDims, int numObservedDataPoints,
                                                                BayesianDataGenerator db, Db2Sampler testerSampleMaker, int numDesiredSamplesFromTesterToSimulateTrueDistribution,
                                                                long maxWaitingTimeForTakingDesiredSamplesMillis) throws IOException{
        //1. upload data if exists...
//    private List<Double[]> fetchAndCompletePersistedGroudTruth(List<Double[]> groundTruth, String path, int numDims, int numObservedDataPoints, String algorithmName, int numDesiredSamples) throws IOException {
        System.out.println("...maxWaitingTimeForTakingDesiredSamplesMillis = " + maxWaitingTimeForTakingDesiredSamplesMillis);
        String algorithmName = testerSampleMaker.getName();
        String generalInfo = "GroundTruthPersisted-dim" + numDims + "-data" + numObservedDataPoints + "-" + algorithmName + "-samples" + numDesiredSamplesFromTesterToSimulateTrueDistribution;
        String fileName = path + generalInfo;

        List<Double[]> oldData = new ArrayList<Double[]>();
        File file = new File(fileName);
        if (file.isFile()) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
            String line;
            int index = 0;
            while ((line = bufferedReader.readLine()) != null) {
                index++;
                String[] split = line.split(" ");
                if (split.length != numDims + 1) throw new RuntimeException("parsing error: " + line);
                if (Integer.parseInt(split[0]) != index) throw new RuntimeException("parsing error: " + line);
                Double[] particle = new Double[numDims];
                for (int j = 0; j < numDims; j++) {
                    particle[j] = Double.parseDouble(split[j + 1]);
                }
                oldData.add(particle);
            }
            System.out.println(index + " entries already exist for ground truth");
            bufferedReader.close();
        } //end if file exists

        //2. if existing data is enough then return it:
        if (oldData.size() >= numDesiredSamplesFromTesterToSimulateTrueDistribution) {
            System.out.println("oldData.size() = " + oldData.size() + "... enough existing data. No new data generated");
            return oldData;
        }

        int newDesiredSamples = numDesiredSamplesFromTesterToSimulateTrueDistribution - oldData.size();
        List<Double[]> newGroundTruth = takeGroundSamples(db, testerSampleMaker, newDesiredSamples, maxWaitingTimeForTakingDesiredSamplesMillis);


        //now append old data to new data and persist....
        newGroundTruth.addAll(oldData);
        PrintStream ps = new PrintStream(new FileOutputStream(fileName));

        for (int i = 0; i < newGroundTruth.size(); i++) {
            //          #index      particle
            Double[] particle = newGroundTruth.get(i);
            StringBuilder sb = new StringBuilder();
            for (Double d : particle) {
                sb.append(d + " ");
            }
            ps.println((i + 1) + " " + sb.toString().trim());
        }
        ps.close();

        System.out.println("#" + newGroundTruth.size() + " Ground truth data generated...");
        return newGroundTruth;
    }

    private List<double[]> createRandomItems(int numQueries, int attributeCount, double minAttribBound, double maxAttribBound) {
        List<double[]> items = new ArrayList<double[]>(numQueries);
        for (int q = 0; q < numQueries; q++) {
            double[] item = makeNewItem(attributeCount, minAttribBound, maxAttribBound);
            items.add(item);
        }
        return items;
    }

    Random random = new Random();

    public double[] makeNewItem(int attributeCount, double minAttribBound, double maxAttribBound) {
        double[] item = new double[attributeCount];
        for (int i = 0; i < attributeCount; i++) {
            item[i] = random.nextDouble() * (maxAttribBound - minAttribBound) + minAttribBound;
        }
        return item;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public List<Double[]> takeGroundSamples(
            BayesianDataGenerator db, Db2Sampler samplerMaker,
            final int numDesiredSamples,
            long maxWaitingTimeForTakingDesiredSamplesMillis //in ms
    ) {
        SamplerInterface sampler = samplerMaker.createSampler(db);
        List<Double[]> groundTruthSamples = new ArrayList<Double[]>(numDesiredSamples); //this will be saved...
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < numDesiredSamples; i++) {
            Double[] sample = sampler.sample();
            Double[] clonedSample = new Double[sample.length];
            System.arraycopy(sample, 0, clonedSample, 0, sample.length);
//            System.out.println("Arrays.toString(sample) = " + Arrays.toString(sample));
            groundTruthSamples.add(clonedSample); //discard samples...
            if (System.currentTimeMillis() - t1 > maxWaitingTimeForTakingDesiredSamplesMillis) break;

//            for (Double[] groundTruthSample : groundTruthSamples) {
//                System.out.println("Arrays.toString(GTS) = " + Arrays.toString(groundTruthSample));
//            }
        }
        return groundTruthSamples;
    }


    public StatInfo meansAndStdErrors(
            List<double[]> fList,
            double[] groundTruthMeansPerF,
            //
            BayesianDataGenerator db,
            Db2Sampler samplerMaker,//SamplerInterface sampler,
            //
            int burnedSamples,
            //
            final int numMinDesiredSamples,
            long maxWaitingTimeForTakingDesiredSamplesMillis, //in ms
            long minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
            int approxNumTimePointsForWhichErrIsPersisted,
            List<Double[]> allGroundTruthSamples
    ) {

//        double timePersistIntervalPerNanoSeconds = (minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis * 1000000 /*milli to nano second*/) / (double) approxNumTimePointsForWhichErrIsPersisted; //intervals after which error vs time points are persisted


        boolean groundTruthMeansAreGiven = groundTruthMeansPerF != null;
        if (groundTruthMeansAreGiven && allGroundTruthSamples!=null) throw new RuntimeException("if truth info is given why do we need samples?");
        boolean samplesShouldBeTaken = true;
        if (!groundTruthMeansAreGiven) {
            if (allGroundTruthSamples!=null) samplesShouldBeTaken = false;  //we are dealing with the ground truth directly... no sample should be taken but means should be calculated
        }
        if (!samplesShouldBeTaken){
            if (burnedSamples!=0) throw new RuntimeException("if samples should not be taken why burning?");
            if (minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis !=0) throw new RuntimeException("if no sample should be taken why waiting?");
            if (numMinDesiredSamples != allGroundTruthSamples.size()) throw new RuntimeException("info mismatch");
        }

        Long timeToTakeFirstSamplesMillis = null; //not necessarily total time if the time analysis takes more time...

        int numFs = fList.size();
        if (!groundTruthMeansAreGiven) {
            groundTruthMeansPerF = new double[numFs];
        }

        if (groundTruthMeansPerF.length != numFs) throw new RuntimeException("size mismatch");

        //burned samples:
        SamplerInterface sampler = samplerMaker.createSampler(db);
        for (int i = 0; i < burnedSamples; i++) {
            sampler.sample(); //discard samples...
        }

        double[] runningSumPerF = new double[numFs];
        double[] runningErrorPerF = new double[numFs];

        List<Double> meanOfErrorVsFirstSamples = new ArrayList<Double>(numMinDesiredSamples);
        List<Double> stdErrOfErrorVsFirstSamples = new ArrayList<Double>(numMinDesiredSamples);

        List<Double> meanOfErrorVsTime = new ArrayList<Double>(approxNumTimePointsForWhichErrIsPersisted);
        List<Double> stdErrOfErrorVsTime = new ArrayList<Double>(approxNumTimePointsForWhichErrIsPersisted);
        List<Long> recordedTimePointsInNano = new ArrayList<Long>(approxNumTimePointsForWhichErrIsPersisted);

        //trying to take the desired number of samples...
        long absoluteStartTimeMillis = System.currentTimeMillis();

        long absoluteStartTimeNanos = System.nanoTime();

//        long absoluteTimeFromLastTimeSavingNano = System.nanoTime();

        int savedTimePoints = 0;

        int takenSamples = 0;

        boolean samplingPerformedInIntendedTimeSuccessfully;

        for (; ; ) {//(int i = 0; i < numMinDesiredSamples; i++) {
            long samplingStarts = System.nanoTime();
            Double[] sample = samplesShouldBeTaken ?
                    sampler.sample()
                    :
                    allGroundTruthSamples.get(takenSamples);
            long samplingEnds = System.nanoTime();
            long singleSampleTimeNano = (samplingEnds - samplingStarts);
            takenSamples++;

            //F(W) = sum_k ( f_k * W_k)
            for (int fId = 0; fId < numFs; fId++) {
                double[] f = fList.get(fId);
                if (f.length != sample.length) throw new RuntimeException("size mismatch"); //debug
                for (int k = 0; k < sample.length; k++) {
                    runningSumPerF[fId] += (f[k] * sample[k]);    //running sum of f(W)
                }

                if (!groundTruthMeansAreGiven) {
                    groundTruthMeansPerF[fId] = runningSumPerF[fId] / (double) takenSamples;
                }

                runningErrorPerF[fId] = Math.abs((runningSumPerF[fId] / (double) takenSamples) - groundTruthMeansPerF[fId]);
            }

            //mean and stdErr for all F
            double rootNumF = Math.sqrt(numFs);
            double mean = 0d;  //E[X]
            double ex2 = 0;  //E[X^2]
            for (int fId = 0; fId < numFs; fId++) {
                double x = runningErrorPerF[fId];
                mean += (x / (double) numFs);
                ex2 += (x * x / (double) numFs);
            }
            double stdErr = (ex2 - mean * mean) / rootNumF;

            if (takenSamples <= numMinDesiredSamples) { //save first samples:
                meanOfErrorVsFirstSamples.add(mean);
                stdErrOfErrorVsFirstSamples.add(stdErr);
            }

            if (takenSamples == numMinDesiredSamples) {
                timeToTakeFirstSamplesMillis = System.currentTimeMillis() - absoluteStartTimeMillis;
            }

//            long timeFromLastTimeRecordInNano = System.nanoTime() - absoluteTimeFromLastTimeSavingNano;
            long nanosFromStart = System.nanoTime() - absoluteStartTimeNanos;
//            if (groundTruthMeansAreGiven){
//            System.out.println("nanosFromStart = " + nanosFromStart);
//            System.out.println("(double)(savedTimePoints * minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis *1000000) / (double) approxNumTimePointsForWhichErrIsPersisted) = " +
//                    (double) (savedTimePoints * minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis * 1000000) / (double) (approxNumTimePointsForWhichErrIsPersisted+1));
//            System.out.println("minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis *1000000 = " + minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis * 1000000);
//            }
            if ((nanosFromStart >= (double) (((long) savedTimePoints) * minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis * 1000000) / (double) (approxNumTimePointsForWhichErrIsPersisted + 1))
//                    && (savedTimePoints <= approxNumTimePointsForWhichErrIsPersisted)
                    && nanosFromStart <= minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis * 1000000
                    ) {
                savedTimePoints++;
//                absoluteTimeFromLastTimeSavingNano = System.nanoTime();

                meanOfErrorVsTime.add(mean);
                stdErrOfErrorVsTime.add(stdErr);
//                recordedTimePointsInNano.add(timeFromLastTimeRecordInNano);
                recordedTimePointsInNano.add(System.nanoTime() - absoluteStartTimeNanos);
            }

            //timeout.
            if (System.currentTimeMillis() - absoluteStartTimeMillis > maxWaitingTimeForTakingDesiredSamplesMillis) {
                samplingPerformedInIntendedTimeSuccessfully = false;
                System.err.println("time out after taking " + takenSamples + " samples.");
                break;
            }

            //successful termination:
            if (
                    System.currentTimeMillis() - absoluteStartTimeMillis >= minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis &&//savedTimePoints >= approxNumTimePointsForWhichErrIsPersisted &&
                            takenSamples >= numMinDesiredSamples) {
                samplingPerformedInIntendedTimeSuccessfully = true;
                break;
            }
        }//end for loop

        Long timeToAccomplishTaskMillis = samplingPerformedInIntendedTimeSuccessfully ?
                System.currentTimeMillis() - absoluteStartTimeMillis
                :
                null;


        return new StatInfo(groundTruthMeansPerF, meanOfErrorVsFirstSamples, stdErrOfErrorVsFirstSamples, meanOfErrorVsTime, stdErrOfErrorVsTime, recordedTimePointsInNano/*, timeToAccomplishTaskMillis*/, timeToTakeFirstSamplesMillis);

//        if (!groundTruthMeansAreGiven) {
//            info.setListOfAllGroundSamples(listOfAllGroundTruthSamplesIfGroundTruthIsBeingCalculated);
//        }
//        return info;
    }


    class StatInfo {
        //general info:

        int numF;
        double[] groundTruthMeans; //this is an array of size #F not persisted but kept in case ground truth is calculated for the first time.

        //calculated info:
        List<Double> means4FirstSamples;
        List<Double> stdErrs4FirstSamples;
        List<Double> means4TimePoints;
        List<Double> stdErrs4TimePoints;
        List<Long> recordedTimePointsInNano;
//        Long totalProcessTimeMillis;
        Long timeToTakeFirstSamples;

        public StatInfo(/*int numParamDims, int numDataPoints, String algorithmName, int maxAllowedSamplingTime,*/ double[] groundTruthMeansPerF,
                        List<Double> meanOfErrorVsFirstSamples, List<Double> stdErrOfErrorVsFirstSamples,
                        List<Double> meanOfErrorVsTime, List<Double> stdErrOfErrorVsTime, List<Long> recordedTimePointsInNano,
//                        Long timeToAccomplishTask,
                        Long timeToTakeFirstSamples) {
//            this.numParamDims = numParamDims;
//            this.numDataPoints = numDataPoints;
//            this.algorithmName = algorithmName;
//            this.maxAllowedSamplingTime = maxAllowedSamplingTime;
            this.groundTruthMeans = groundTruthMeansPerF;
            this.numF = groundTruthMeans.length;

            this.means4FirstSamples = meanOfErrorVsFirstSamples;
            this.stdErrs4FirstSamples = stdErrOfErrorVsFirstSamples;

            this.means4TimePoints = meanOfErrorVsTime;
            this.stdErrs4TimePoints = stdErrOfErrorVsTime;
            this.recordedTimePointsInNano = recordedTimePointsInNano;

            this.timeToTakeFirstSamples = timeToTakeFirstSamples;
//            this.totalProcessTimeMillis = timeToAccomplishTask;

            if (means4FirstSamples.size() != stdErrs4FirstSamples.size()) throw new RuntimeException("size mismatch");
            if ((means4TimePoints.size() != stdErrs4TimePoints.size()) || (means4TimePoints.size() != this.recordedTimePointsInNano.size()))
                throw new RuntimeException("size mismatch");

        }

        private String generalInfo(int numParamDims, int numDataPoints, String algorithmName/*, int maxAllowedSamplingTime*/) {
            return "dim" + numParamDims + "-data" + numDataPoints + "-f" + numF /*+ "-maxT" + maxAllowedSamplingTime */ + "-" + algorithmName;
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
                ps.println((i + 1) + "\t" + recordedTimePointsInNano.get(i) / 1000000 + "\t" + means4TimePoints.get(i) + "\t" + stdErrs4TimePoints.get(i));
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
        public TotalTimeKeeper(Db2Sampler[] samplers, int[] dimsArray, int[] dataArray, String outputDirectoryPath) {
            this.outputDirectoryPath = outputDirectoryPath;

            this.dimsArray = new int[dimsArray.length];
            this.dataArray = new int[dataArray.length];
            System.arraycopy(dimsArray, 0, this.dimsArray, 0, dimsArray.length);
            System.arraycopy(dataArray, 0, this.dataArray, 0, dataArray.length);
            Arrays.sort(this.dimsArray);
            Arrays.sort(this.dataArray);

            dimIndexDataIndexAlgTime = new HashMap[dimsArray.length][dataArray.length];

            this.samplerNames = new String[samplers.length];
            for (int i = 0; i < samplers.length; i++) {
                this.samplerNames[i] = samplers[i].getName();
            }

            for (int i = 0; i < dimsArray.length; i++) {
                for (int j = 0; j < dataArray.length; j++) {
                    dimIndexDataIndexAlgTime[i][j] = new HashMap<String, Long>(samplers.length);
                }
            }
        }

        public void persist(int dim, int data, String samplerName, Long timeMillis) throws FileNotFoundException {
            int dimIndex = Arrays.binarySearch(dimsArray, dim);
            int dataIndex = Arrays.binarySearch(dataArray, data);
            if (dimIndexDataIndexAlgTime[dimIndex][dataIndex].put(samplerName, timeMillis) != null) throw new RuntimeException(samplerName + " already exists!");
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
            if (n!= timeList.size()) throw new RuntimeException("size mismatch");
            for (int i = 0; i < n; i++) {
                ps.println(otherElementList.get(i) + "\t" + timeList.get(i));
            }

            ps.close();
        }

    }
}


