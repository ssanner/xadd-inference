package hgm.poly.pref.reports;

import hgm.asve.Pair;
import hgm.poly.integral.OneDimFunction;
import hgm.poly.pref.*;
import hgm.poly.pref.reports.db.SyntheticDistributionUtils;
import hgm.poly.sampling.PolyGibbsSampler;
import hgm.poly.sampling.SamplerInterface;
import hgm.poly.vis.FunctionVisualizer;
import hgm.preference.Choice;
import hgm.preference.Preference;
import hgm.preference.PreferenceLearning;
import hgm.preference.db.DummyFeasiblePreferenceDatabase;
import hgm.preference.db.PartialPreferenceDatabase;
import hgm.preference.db.PreferenceDatabase;
import hgm.preference.predict.XaddPolytopePrefLearningPredictor;
import hgm.preference.predict.XaddPolytopePrefLearningPredictorUsingGibbsWithCDFsPerSample;
import hgm.preference.predict.PreferenceLearningPredictor;
import hgm.preference.predict.TrueSkillPrefLearningPredictor;
import hgm.sampling.*;
import hgm.utils.Utils;
import org.junit.Test;
import xadd.XADD;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * Written before UAI 2014
 */
//todo complete this file..............
public class ReportGPolyGibbsTrueSkillMHRejectionOnSynthesizedDB {
    public static final String REPORT_PATH = "./test/hgm/poly/pref/reports/";

    public static boolean DEBUG = false;

    public static void main(String[] args) throws IOException {
        ReportGPolyGibbsTrueSkillMHRejectionOnSynthesizedDB instance = new ReportGPolyGibbsTrueSkillMHRejectionOnSynthesizedDB();
        instance.bigNewTest();
    }

    @Test
    public void predictionTestOnDummyFeasibleModel() throws IOException {
        double indicatorNoise = 0.0;
        boolean reduceLP = true;
        double relativeLeafValueBelowWhichRegionsAreTrimmed = -0.01;
        int numberOfSamples = 1000;

        System.out.println("#Samples:" + numberOfSamples + "\t relLeafThreshold:"
                + relativeLeafValueBelowWhichRegionsAreTrimmed + "\t indicator noise:" + indicatorNoise);

        List<Pair<String, PreferenceLearningPredictor>> predictors = new ArrayList<Pair<String, PreferenceLearningPredictor>>(
                Arrays.asList(new Pair<String, PreferenceLearningPredictor>("poly",
                        new XaddPolytopePrefLearningPredictorUsingGibbsWithCDFsPerSample(indicatorNoise, reduceLP,
                                numberOfSamples,
                                relativeLeafValueBelowWhichRegionsAreTrimmed, 0, 0)),
                        new Pair<String, PreferenceLearningPredictor>("true.skill",
                                new TrueSkillPrefLearningPredictor(0))));

        preferencePredictionOnDummyFeasibleModel(predictors, numberOfSamples);
    }

    @Test
    public void predictionTestOnDummyFeasibleModelUsingPolyXadPolyMetropolisAndProjection() throws IOException {
        double indicatorNoise = 0.1;
        boolean reduceLP = true;
        double relativeLeafValueBelowWhichRegionsAreTrimmed = -0.01;
        int burnedSamples = 100;
        Integer maxGateConstraintViolation = Integer.MAX_VALUE;
        int[] numberOfSamplesArray = new int[]{100 + burnedSamples, 500 + burnedSamples, 10000 + burnedSamples}; // 100 for burn in

        for (int numberOfSamples : numberOfSamplesArray) {

            System.out.println("#Samples:" + numberOfSamples + "\t relLeafThreshold:"
                    + relativeLeafValueBelowWhichRegionsAreTrimmed + "\t indicator noise:" + indicatorNoise);

            List<Pair<String, PreferenceLearningPredictor>> predictors = new ArrayList<Pair<String, PreferenceLearningPredictor>>(
                    Arrays.asList(
                            new Pair<String, PreferenceLearningPredictor>("new.poly",
                                    new GPolyPreferenceLearningPredictorUsingGibbsSampler(indicatorNoise,
                                            numberOfSamples, burnedSamples, maxGateConstraintViolation, -PreferenceLearning.C, PreferenceLearning.C))
                            , new Pair<String, PreferenceLearningPredictor>("xadd.poly",
                            new XaddPolytopePrefLearningPredictorUsingGibbsWithCDFsPerSample(indicatorNoise,
                                    reduceLP, numberOfSamples,
                                    relativeLeafValueBelowWhichRegionsAreTrimmed, 0,
                                    burnedSamples))
                            , new Pair<String, PreferenceLearningPredictor>("true.skill",
                            new TrueSkillPrefLearningPredictor(0))
                            , new Pair<String, PreferenceLearningPredictor>("metro",
                            new XaddPolytopePrefLearningPredictor(indicatorNoise, reduceLP,
                                    numberOfSamples,
                                    relativeLeafValueBelowWhichRegionsAreTrimmed, 0,
                                    burnedSamples) {

                                @Override
                                public XaddSampler makeNewSampler(XADD context,
                                                                  XADD.XADDNode posterior,
                                                                  VarAssignment initAssignment) {
                                    return new MetropolisHastingsSampler(context, posterior,
                                            null/*initAssignment*/);
                                }
                            }), new Pair<String, PreferenceLearningPredictor>("rej",
                            new XaddPolytopePrefLearningPredictor(indicatorNoise, reduceLP,
                                    numberOfSamples,
                                    relativeLeafValueBelowWhichRegionsAreTrimmed, 0,
                                    burnedSamples) {

                                @Override
                                public XaddSampler makeNewSampler(XADD context,
                                                                  XADD.XADDNode posterior,
                                                                  VarAssignment initAssignment) {
                                    return new RejectionSampler(context, posterior,
                                            null/*initAssignment*/, 1);
                                }
                            })
                    ));

            preferencePredictionOnDummyFeasibleModel(predictors, numberOfSamples - burnedSamples);
        }
    }

    // #Samples vs Loss
    /*@Test
    public void roleOfSampleNumInPolytopePrefPredictionTestOnDummyFeasibleModelAndGibbs() {
        double indicatorNoise = 0.0;
        boolean reduceLP = true;
        double relativeLeafValueBelowWhichRegionsAreTrimmed = -0.01;
        int burnedSamples = 100;
        int maxNumberOfSamples = 10000 + burnedSamples;
        int[] sampleNums = new int[]{1, 10, 50, 100, 200, 500, 1000, 2000, 4000, 6000, 8000, 10000};

        System.out.println("#BurnedSamples=" + burnedSamples + "\t#samples:" + Arrays.toString(sampleNums)
                + "\t relLeafThreshold:" + relativeLeafValueBelowWhichRegionsAreTrimmed + "\t indicator noise:"
                + indicatorNoise);

        testingTheEffectOfNumberOfSamplesInPreferencePredictionOnDummyFeasibleModel(burnedSamples,
                new XaddPolytopePrefLearningPredictorUsingGibbsWithCDFsPerSample(
                        indicatorNoise,
                        reduceLP,
                        maxNumberOfSamples,
                        relativeLeafValueBelowWhichRegionsAreTrimmed,
                        0, burnedSamples),
                sampleNums);
    }*/

    /**
     * *******************************
     */
    public static void preferencePredictionOnDummyFeasibleModel(
            List<Pair<String /* name */, PreferenceLearningPredictor>> predictors, int numberOfSamplesJustForInfo) throws IOException {
        System.out.println(" #constraints and/or #dims vs. Loss");
        int numberOfItems = 500; // shouldn't have any significant effect (?) unless if its too small, dummy items will be repeated...
        int[] numDimsArray = new int[]{2, 3, 9};//{5, 10, 20};
        int[] numConstraintsArray = new int[]{3, 10, 20};
        int numberOfTestPrefs = 600;  //this used to be the number of tests per data base (?)
        int numRepeatingEachExperiment = 10;
        Integer numSamplesToEstimateRealDistribution = 1000;
        HashMap<String, ArrayList<Double>> result_maps = new HashMap<String, ArrayList<Double>>();

        for (int numDims : numDimsArray) {

            for (int numTrainingConstraints : numConstraintsArray) {

                Map<String, Double> predictor2averageLossMap = new HashMap<String, Double>();
                Map<String, Double> predictor2expectedSquareLoss = new HashMap<String, Double>();
                for (Pair<String, PreferenceLearningPredictor> predictor : predictors) {
                    predictor2averageLossMap.put(predictor.getFirstEntry(), 0d);
                    predictor2expectedSquareLoss.put(predictor.getFirstEntry(), 0d);
                }

                for (int experimentRepetitionCounter = 0; experimentRepetitionCounter < numRepeatingEachExperiment; experimentRepetitionCounter++) {

                    PreferenceDatabase completeDatabase = SyntheticDistributionUtils.fetchOrGenerateTrainTestPreferenceDbDistribution("1" /*prior*/,
                            -PreferenceLearning.C, PreferenceLearning.C,
                            0d, 5d,
                            numDims, numberOfItems, numTrainingConstraints, numberOfTestPrefs, numSamplesToEstimateRealDistribution, 0.1, 6, false);
                    /*PreferenceDatabase completeDatabase = new DummyFeasiblePreferenceDatabase(
                            -PreferenceLearning.C,
                            PreferenceLearning.C,
                            0d,
                            5d,
                            numConstraints + numberOfTestComparisonsPerDatabase *//* more preferences used for testing *//*,
                            numDims, numberOfItems *//* number of items *//*);*/

                    //NOTE: the first 'numTrainingConstraints' entries are for training and the rest are for test
                    PreferenceDatabase trainingDb = new PartialPreferenceDatabase(completeDatabase, numTrainingConstraints);

                    for (Pair<String, PreferenceLearningPredictor> strPredictor : predictors) { // for different predictors same date is used for fairness
                        String predName = strPredictor.getFirstEntry();
                        PreferenceLearningPredictor predictor = strPredictor.getSecondEntry();

                        long timeStampStartTrain = System.currentTimeMillis();

                        List<Pair<String, Double>> info = predictor.learnToPredict(trainingDb);

                        long time5testingStart = System.currentTimeMillis();

                        long trainTime = time5testingStart - timeStampStartTrain;

                        int numLosses = 0;
                        double probabilityOfTheCorrectAnswer = 0d;

                        for (int testCounter = 0; testCounter < numberOfTestPrefs; testCounter++) {

                            Preference testPref = completeDatabase.getPreferenceResponses()
                                    .get(numTrainingConstraints + testCounter);
                            Integer aId = testPref.getItemId1();
                            Integer bId = testPref.getItemId2();
                            Double[] a = completeDatabase.getItemAttributeValues(aId);
                            Double[] b = completeDatabase.getItemAttributeValues(bId);

                            Choice predictedChoice = predictor.predictPreferenceChoice(a, b);
                            double probabilityOfTheChosenItem = predictedChoice.equals(Choice.FIRST) ?
                                    predictor.probabilityOfFirstItemBeingPreferredOverSecond(a, b)
                                    :
                                    predictor.probabilityOfFirstItemBeingPreferredOverSecond(b, a);

                            Choice correctChoice = testPref.getPreferenceChoice();
                            if (!predictedChoice.equals(correctChoice)) {
                                numLosses++;
                                probabilityOfTheCorrectAnswer += (1 - probabilityOfTheChosenItem);
                            } else {
                                probabilityOfTheCorrectAnswer += probabilityOfTheChosenItem;
                            }

                        }// end test for

                        probabilityOfTheCorrectAnswer /= (double) numberOfTestPrefs;

                        double averageLoss = numLosses / (double) numberOfTestPrefs;
                        System.out.println("[" + fixedLengthStr(predName, 12) + ":] averageLoss(Dim. " + numDims + " : TrainConstr. " + numTrainingConstraints + " #samples. "
                                + numberOfSamplesJustForInfo + "|Train Time:" + trainTime + ") = " + averageLoss + "\t prob.correct.answer: " + probabilityOfTheCorrectAnswer);

                        predictor2averageLossMap.put(predName, predictor2averageLossMap.get(predName)
                                + (averageLoss / (double) numRepeatingEachExperiment));
                        predictor2expectedSquareLoss.put(predName, predictor2expectedSquareLoss.get(predName)
                                + (averageLoss * averageLoss / (double) numRepeatingEachExperiment));

                        // addToHashMap(result_maps, "predname_" + predName, predName);
                        addToHashMap(result_maps, "experimentRepetitionCounter_" + predName,
                                experimentRepetitionCounter);
                        addToHashMap(result_maps, "numDims_" + predName, numDims);
                        addToHashMap(result_maps, "numConstraints_" + predName, numTrainingConstraints);
                        addToHashMap(result_maps, "takenSamples_" + predName, numberOfSamplesJustForInfo);
                        addToHashMap(result_maps, "numberOfTestComparisonsPerDatabase_" + predName,
                                numberOfTestPrefs);
                        addToHashMap(result_maps, "averageLoss_" + predName, averageLoss);
                        addToHashMap(result_maps, "probCorrectAnswer_" + predName, probabilityOfTheCorrectAnswer);
                        addToHashMap(result_maps, "trainTime_" + predName, time5testingStart - timeStampStartTrain);

                        Utils.writeMatMap("results.mat", result_maps);

                        // addToHashMap(result_maps, "averageLoss", averageLoss);

                        // long time6testingEnd = System.currentTimeMillis();

						/* // #Dims \t\t #Constraints \t\t time for 1. Posterior calc \t\t posterior nodes \t\t Elapsed time for 3. w_i - CDF
                         * calculated: \t\t Elapsed time for 4. sampling: System.out.println("[" + predName + ":]\t" + numDims + " \t\t " +
						 * numConstraints + "\t\t\t" + info + //(time2posteriorCalculated - time1start) + "\t\t (" + posteriorNodeCount + // ")\t\t" +
						 * //(time4samplerInitialized - time3posteriorReduced) + // "\t\t " + (time5samplesTaken - time3posteriorReduced) + "\t\t" +
						 * (time6testingEnd - time5testingStart)); */

                    }// end predictor loop
                    System.out.print("\n");

                    SyntheticDistributionUtils.savePrefDb(completeDatabase, "completeDatabase_" + experimentRepetitionCounter + "_"
                            + numTrainingConstraints + "_" + numDims + ".mat");

                }// end repetition loop
                Map<String, Double> pred2Sigma = new HashMap<String, Double>();// root E(X^2) - E(X)^2
                for (String predName : predictor2averageLossMap.keySet()) {
                    pred2Sigma.put(predName,
                            Math.sqrt(predictor2expectedSquareLoss.get(predName)
                                    - Math.pow(predictor2averageLossMap.get(predName), 2d)));
                }
                System.out.println("\n(#DIM:" + numDims + "/CNST:" + numTrainingConstraints
                        + ") \n>>> predictor2AverageLossMap = " + predictor2averageLossMap
                        + "\t\t predicator2sigma = " + pred2Sigma);

            } // end numConstraints for
        } // end numDim for

    }

    private static void addToHashMap(HashMap<String, ArrayList<Double>> maps, final String key, final double val) {
        String name = key.replace(".", "_");
        if (!maps.containsKey(name)) {
            maps.put(name, new ArrayList<Double>());
        }
        maps.get(name).add(val);
    }

    ///////////////     T E S T S     A F T E R    U A I   D E A D L I N E
    ///////////////////////////////////////////////////// /\  /////////////////////////////////////////////////////
    ///////////////////////////////////////////////////// ||  /////////////////////////////////////////////////////
    //////////////////////////////////////////////////// /  \ /////////////////////////////////////////////////////
    interface Db2Sampler {
        SamplerInterface createSampler(PreferenceDatabase db);

        String getName();
    }

    @Test
    public void bigNewTest() throws FileNotFoundException {
        final double indicatorNoise = 0.4;
        final int maxGatingConditionViolation = Integer.MAX_VALUE;

        /*//----------------------------------------------------------------------------------------------
        Db2Sampler baselinePolyDb2Sampler = new Db2Sampler() {
            @Override
            public SamplerInterface createSampler(PreferenceDatabase db) {
                GPolyPreferenceLearning learning = new GPolyPreferenceLearning(db, indicatorNoise, "w");
                // Pr(W | R^{n+1})
                PolytopesHandler posterior = learning.computePosteriorWeightVector(maxGatingConditionViolation);
                //now I sample from it:
                SamplerInterface sampler = PolyGibbsSampler.makeGibbsSampler(posterior, //todo rejection based sampling should be used instead...
                        -GPolyPreferenceLearning.C,
                        GPolyPreferenceLearning.C, null);

                return sampler;
            }
            @Override
            public String getName() {
                return "gated";
            }
        };*/
        //----------------------------------------------------------------------------------------------
        Db2Sampler gPolyDb2Sampler = new Db2Sampler() {
            @Override
            public SamplerInterface createSampler(PreferenceDatabase db) {
                GPolyPreferenceLearning learning = new GPolyPreferenceLearning(db, indicatorNoise, "w");

                // Pr(W | R^{n+1})
                PolytopesHandler posterior = learning.computePosteriorWeightVector(maxGatingConditionViolation);
//                if (VISUALIZE) FunctionVisualizer.visualize(posterior, -10, 10, 0.1, "posterior");

                //now I sample from it:
                SamplerInterface sampler = GatedGibbsPolytopesSampler.makeGibbsSampler(posterior, //todo rejection based sampling should be used instead...
                        -GPolyPreferenceLearning.C,
                        GPolyPreferenceLearning.C, null);

                return sampler;
            }

            @Override
            public String getName() {
                return "gated";
            }
        };
        //----------------------------------------------------------------------------------------------
        Db2Sampler gCleverPolyDb2Sampler = new Db2Sampler() {
            @Override
            public SamplerInterface createSampler(PreferenceDatabase db) {
                GPolyPreferenceLearning learning = new GPolyPreferenceLearning(db, indicatorNoise, "w");

                // Pr(W | R^{n+1})
                PolytopesHandler posterior = learning.computePosteriorWeightVector(maxGatingConditionViolation);
//                if (VISUALIZE) FunctionVisualizer.visualize(posterior, -10, 10, 0.1, "posterior");

                //now I sample from it:
                SamplerInterface sampler = CleverGatedGibbsPolytopesSampler.makeCleverGibbsSampler(posterior, //todo rejection based sampling should be used instead...
                        -GPolyPreferenceLearning.C,
                        GPolyPreferenceLearning.C, null);

                return sampler;
            }

            @Override
            public String getName() {
                return "clever";
            }
        };
        //----------------------------------------------------------------------------------------------
        Db2Sampler fullGibbsPolyDb2Sampler = new Db2Sampler() {
            @Override
            public SamplerInterface createSampler(PreferenceDatabase db) {
                GPolyPreferenceLearning learning = new GPolyPreferenceLearning(db, indicatorNoise, "w");

                // Pr(W | R^{n+1})
                PolytopesHandler posterior = learning.computePosteriorWeightVector(maxGatingConditionViolation);
//                if (VISUALIZE) FunctionVisualizer.visualize(posterior, -10, 10, 0.1, "posterior");

                //now I sample from it:
                SamplerInterface sampler = FullGibbsPolytopesSampler.makeFullGibbsSampler(posterior, //todo rejection based sampling should be used instead...
                        -GPolyPreferenceLearning.C,
                        GPolyPreferenceLearning.C, null);

                return sampler;
            }

            @Override
            public String getName() {
                return "full";
            }
        };
        //----------------------------------------------------------------------------------------------
        Db2Sampler xaddRejSampler = new Db2Sampler() {
            boolean reduceLP = true;

            @Override
            public SamplerInterface createSampler(PreferenceDatabase db) {
                XADD context = new XADD();
                PreferenceLearning learning = new PreferenceLearning(context, db, indicatorNoise, "w", 0/*epsilon*/);
                //                        long time1start = System.currentTimeMillis();
                // Pr(W | R^{n+1})
                XADD.XADDNode posterior = learning.computePosteriorWeightVector(reduceLP, -1/*relativeLeafValueBelowWhichRegionsAreTrimmed*/);
                XaddPolytopePrefLearningPredictor.fixVarLimits(context, posterior, -PreferenceLearning.C, PreferenceLearning.C); //todo: do something better...
//                        long time2posteriorCalculated = System.currentTimeMillis();
                final XaddSampler sampler = makeNewSampler(context, posterior, learning.generateAWeightVectorHighlyProbablePosteriorly());
                return new SamplerInterface() {
                    @Override
                    public Double[] sample() throws SamplingFailureException {
                        return sampler.sample().getContinuousVarAssignAsArray("w");
                    }
                };
            }

            public XaddSampler makeNewSampler(XADD context,
                                              XADD.XADDNode posterior,
                                              VarAssignment initAssignment) {
                return new RejectionSampler(context, posterior,
                        null/*initAssignment*/, 1);
            }

            @Override
            public String getName() {
                return "rej";
            }
        };
        //----------------------------------------------------------------------------------------------
        Db2Sampler xaddMHSampler = new Db2Sampler() {
            boolean reduceLP = true;

            @Override
            public SamplerInterface createSampler(PreferenceDatabase db) {
                XADD context = new XADD();
                PreferenceLearning learning = new PreferenceLearning(context, db, indicatorNoise, "w", 0/*epsilon*/);
                //                        long time1start = System.currentTimeMillis();
                // Pr(W | R^{n+1})
                XADD.XADDNode posterior = learning.computePosteriorWeightVector(reduceLP, -1/*relativeLeafValueBelowWhichRegionsAreTrimmed*/);
                XaddPolytopePrefLearningPredictor.fixVarLimits(context, posterior, -PreferenceLearning.C, PreferenceLearning.C); //todo: do something better...
//                        long time2posteriorCalculated = System.currentTimeMillis();
                final XaddSampler sampler = makeNewSampler(context, posterior, learning.generateAWeightVectorHighlyProbablePosteriorly());
                return new SamplerInterface() {
                    @Override
                    public Double[] sample() throws SamplingFailureException {
                        return sampler.sample().getContinuousVarAssignAsArray("w");
                    }
                };
            }

            public XaddSampler makeNewSampler(XADD context,
                                              XADD.XADDNode posterior,
                                              VarAssignment initAssignment) {
                return new MetropolisHastingsSampler(context, posterior,
                        null/*initAssignment*/, 1);
            }

            @Override
            public String getName() {
                return "metro";
            }
        };
        //----------------------------------------------------------------------------------------------

        List<Db2Sampler> samplerMakers = Arrays.asList(
                xaddRejSampler,
                xaddMHSampler,
//                fullGibbsPolyDb2Sampler,
                gPolyDb2Sampler
//                ,gCleverPolyDb2Sampler
        );

        testEffectOfNumSamplesWrtDimsAndConstraints(samplerMakers);
//        testTimeVsNumConstraints(samplerMakers, 7/*dim fixed*/);

        //*****************************************************************************************

        System.out.println("that was all folk");

    }

    private void testEffectOfNumSamplesWrtDimsAndConstraints(List<Db2Sampler> samplerMakers) throws FileNotFoundException {
        int numberOfItems = 500; // shouldn't have any significant effect (?) unless if its too small, dummy items will be repeated...
        int[] numDimsArray = new int[]{6};
        int[] numConstraintsArray = new int[]{2, 3, 5, 6, 7, 8, 9, 10, 15, 18, 22, 25};
        int numSamples = 10000;
        double minAttribBound = 0d;
        double maxAttribBound = 5d;
        int numQueries = 1;
        int burnedSamples = 10;

        Map<String/*dim-alg*/, List<Pair<Integer /*numConstraints*/, Long /*calc. time*/>>> dimAlg2TimesMap =
                new HashMap<String, List<Pair<Integer, Long>>>();

        for (int numDims : numDimsArray) {

            for (Db2Sampler samplerMaker : samplerMakers) {
                dimAlg2TimesMap.put(timeContainerKey(numDims, samplerMaker), new ArrayList<Pair<Integer, Long>>());
            }

            for (int numConstraints : numConstraintsArray) {
                System.out.println("numDims = " + numDims);
                System.out.println("numConstraints = " + numConstraints);

                DummyFeasiblePreferenceDatabase db = new DummyFeasiblePreferenceDatabase(
                        -PreferenceLearning.C,
                        PreferenceLearning.C,
                        minAttribBound, //TODO:IMPORTANT check the effect of negative x_i bound...
                        maxAttribBound,
                        numConstraints, // + numberOfTestComparisonsPerDatabase /* more preferences used for testing */,
                        numDims, numberOfItems /* number of items */);

                //todo: check SyntheticDistributionUtils sample generator...

                List<double[]> as = createRandomItems(numQueries, db);
                List<double[]> bs = createRandomItems(numQueries, db);
//                double[] onesObj = new double[numDims];
//                double[] zerosObj = new double[numDims];
//                Arrays.fill(onesObj, 1.0);
//                Arrays.fill(zerosObj, 0.0);
//                as.add(onesObj);
//                List<double[]> bs = new ArrayList<double[]>();
//                bs.add(zerosObj);


                List<Double> groundTruthMeans = null;


                for (Db2Sampler samplerMaker : samplerMakers) {
                    StatInfo statInfo = meansAndStdErrors(as, bs, groundTruthMeans, db, samplerMaker, burnedSamples, numSamples);//gPolyDb2Sampler, 100, numSamples);
                    groundTruthMeans = statInfo.groundTruthMeans; //So, it is calculated only for the first sampler...
                    statInfo.persistMeanStdErr(REPORT_PATH, numDims, numConstraints, as.size(), samplerMaker.getName());

                    String key = timeContainerKey(numDims, samplerMaker);
                    List<Pair<Integer, Long>> constraintsAndTimes = dimAlg2TimesMap.get(key);
                    constraintsAndTimes.add(new Pair<Integer, Long>(numConstraints, statInfo.processTime));
                    persistConstraintsTimes(REPORT_PATH + key + "-q" + numQueries, constraintsAndTimes);
                    System.out.println(samplerMaker.getName() + ".time = " + statInfo.processTime);
                }
//                FunctionVisualizer.visualize(stdErr, 0, numSamples, 1, statType + " #dim:" + numDims + " #cnstrnt:" + numConstraints);
            }
        }

    }

    private void persistConstraintsTimes(String fileName, List<Pair<Integer, Long>> constraintsAndTimes) throws FileNotFoundException {
        PrintStream ps = new PrintStream(new FileOutputStream(fileName));
        for (Pair<Integer, Long> ct : constraintsAndTimes) {
            ps.println(ct.getFirstEntry() + "\t" + ct.getSecondEntry());
        }
        ps.close();
    }

    private String timeContainerKey(int numDims, Db2Sampler samplerMaker) {
        return "dims" + numDims + "-" + samplerMaker.getName();
    }

    private List<double[]> createRandomItems(int numQueries, DummyFeasiblePreferenceDatabase db) {
        List<double[]> items = new ArrayList<double[]>(numQueries);
        for (int q = 0; q < numQueries; q++) {
            Double[] item = db.makeNewItem();

            //this is not good:
            double[] item2 = new double[item.length];
            for (int i = 0; i < item.length; i++) {
                item2[i] = item[i];
            }
            items.add(item2);
        }
        return items;
    }


    public StatInfo meansAndStdErrors(
            List<double[]> aList,
            List<double[]> bList,
            List<Double> groundTruthMeans,
            //
            PreferenceDatabase db,
            Db2Sampler samplerMaker,//SamplerInterface sampler,
            //
            int burnedSamples, final int numSamples) {

        int numQueries = aList.size();
        if (numQueries != bList.size()) throw new RuntimeException("size mismatch");
        if (groundTruthMeans != null && groundTruthMeans.size() != bList.size())
            throw new RuntimeException("size mismatch");

        double[][] values = new double[numQueries][numSamples];
//        double[] _EX = new double[numSamples]; //_EX[i] = mean of f(A, B, W) after i samples (over all queries A, B)
//        double[] stdErr = new double[numSamples];

        long t1 = System.currentTimeMillis();
        SamplerInterface sampler = samplerMaker.createSampler(db);
        for (int i = 0; i < burnedSamples; i++) {
            sampler.sample(); //discard samples...
        }


        for (int i = 0; i < numSamples; i++) {
            Double[] sample = sampler.sample();

            //f(a, b, W) = sum_k (a_k - b_k)*W_k = u(a|W) - u(b|W)
            for (int qId = 0; qId < numQueries; qId++) {
                double[] a = aList.get(qId);
                double[] b = bList.get(qId);
                if (a.length != b.length || a.length != sample.length)
                    throw new RuntimeException("size mismatch"); //debug
                for (int k = 0; k < sample.length; k++) {
                    values[qId][i] += Math.abs((a[k] - b[k]) * sample[k]);    //f(a, b, W)
                }
            }
        }

        if (DEBUG) {
            //hack!
            if (sampler instanceof AbstractPolytopesSampler) {
                System.out.println("debugNumUnsuccessfulSamplings = " + ((AbstractPolytopesSampler) sampler).debugNumUnsuccessfulSamplings);
            }
        }

        long t2 = System.currentTimeMillis();

        //I reuse the values and put the running sums in them.
        for (int i = 1; i < numSamples; i++) {
            for (int qId = 0; qId < numQueries; qId++) {
                values[qId][i] += values[qId][i - 1];
            }
        }

        //make ground truth means if do not exist...
        if (groundTruthMeans == null) {
            groundTruthMeans = new ArrayList<Double>(numQueries);
            for (int qId = 0; qId < numQueries; qId++) {
                groundTruthMeans.add(values[qId][numSamples - 1] / (double) numSamples);
            }
        }

        //Now I put |means(till this sample) - groundTruthMeans| in tha values
        for (int i = 0; i < numSamples; i++) {
            for (int qId = 0; qId < numQueries; qId++) {
                values[qId][i] = Math.abs((values[qId][i] / (double) (i + 1)) - (groundTruthMeans.get(qId)));
            }
        }

        //Now the means of values w.r.t. different <a,b>s:
        double[] means = new double[numSamples];  //E[X]
        double[] stdErrs = new double[numSamples];
        double rootNumQ = Math.sqrt(numQueries);
        for (int i = 0; i < numSamples; i++) {
            double ex2 = 0;  //E[X^2]
            for (int qId = 0; qId < numQueries; qId++) {
                double x = values[qId][i];
                means[i] += (x / (double) numQueries);
                ex2 += (x * x / (double) numQueries);
            }

            stdErrs[i] = (ex2 - means[i] * means[i]) / rootNumQ;
        }

        return new StatInfo(groundTruthMeans, means, stdErrs, (int) (t2 - t1));
    }

    class StatInfo {
        List<Double> groundTruthMeans;
        double[] means;
        double[] stdErrs;
        long processTime;

        StatInfo(List<Double> groundTruthMeans, double[] means, double[] stdErrs, long processTime) {
            this.groundTruthMeans = groundTruthMeans;
            this.means = means;
            this.stdErrs = stdErrs;
            this.processTime = processTime;
        }

        public void persistMeanStdErr(String path, int numDims, int numConstraints, int numQueries, String algorithm) throws FileNotFoundException {
            PrintStream ps;
            String outputFileName = path + "dims" + numDims + "-cnstrs" + numConstraints + "-samples" + means.length + "-q" + numQueries + "-" + algorithm;

            ps = new PrintStream(new FileOutputStream(outputFileName));

            for (int i = 0; i < means.length; i++) {
                ps.println((i + 1) + "\t" + means[i] + "\t" + stdErrs[i]);
            }

            ps.close();
        }

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Deprecated
    @Test
    public void visualizationOfEffectOfNumberOfSamples() {
        Stat statType = Stat.MEAN;

        double indicatorNoise = 0.3;
        int maxGatingConditionViolation = Integer.MAX_VALUE;
        int numberOfItems = 500; // shouldn't have any significant effect (?) unless if its too small, dummy items will be repeated...
        int[] numDimsArray = new int[]{2, 7};
        int[] numConstraintsArray = new int[]{2, 10, 40};
        int numSamples = 100000;

        for (int numDims : numDimsArray) {
            for (int numConstraints : numConstraintsArray) {
                System.out.println("numDims = " + numDims);
                System.out.println("numConstraints = " + numConstraints);

                PreferenceDatabase db = new DummyFeasiblePreferenceDatabase(
                        -PreferenceLearning.C,
                        PreferenceLearning.C,
                        0d, //TODO:IMPORTANT check the effect of negative x_i bound...
                        5d,
                        numConstraints, // + numberOfTestComparisonsPerDatabase /* more preferences used for testing */,
                        numDims, numberOfItems /* number of items */);

                //todo: check SyntheticDistributionUtils sample generator...


                GPolyPreferenceLearning learning = new GPolyPreferenceLearning(db, indicatorNoise, "w");

                // Pr(W | R^{n+1})
                PolytopesHandler posterior = learning.computePosteriorWeightVector(maxGatingConditionViolation);

//                if (VISUALIZE) FunctionVisualizer.visualize(posterior, -10, 10, 0.1, "posterior");

                //now I sample from it:
                GatedGibbsPolytopesSampler sampler = GatedGibbsPolytopesSampler.makeGibbsSampler(posterior, //todo rejection based sampling should be used instead...
                        -GPolyPreferenceLearning.C,
                        GPolyPreferenceLearning.C, null);


                OneDimFunction stdErr = statistics(statType, sampler, 100, numSamples);
                FunctionVisualizer.visualize(stdErr, 0, numSamples, 1, statType + " #dim:" + numDims + " #cnstrnt:" + numConstraints);
            }
        }

        System.out.println("that was all folk");


    }
    // ***
    // #samples vs. loss


    enum Stat {MEAN, STD_DEV, STD_ERR;}

    @Deprecated
    public static OneDimFunction statistics(
            Stat statType,
            SamplerInterface sampler,
            int burnedSamples, final int numSamples) {
        System.out.println("Effect of number of samples...");

        double sumX = 0d;
        double sumX2 = 0d;
        final double[] _EX = new double[numSamples];
        final double[] _EX2 = new double[numSamples];

        for (int i = 0; i < burnedSamples; i++) {
            sampler.sample(); //discard samples...
        }
        for (int i = 0; i < numSamples; i++) {
            Double[] sample = sampler.sample();


            double sampleSum = 0d;
            for (Double w : sample) {
                sampleSum += w;
            }
            double _X = sampleSum / (double) sample.length; // mean of all weight elements (of all samples)
/* I have samples W_1, ... W_n,
pr(q_ab|W_j)=sum_i (a_i-b_i)*w_ij
a0 = [1, 1, 1..]
b0 = [0, 0, ...]
pr(q_a0b0|W_j) = sum_i w_ij = mean W_j?
sum_m mean(Sample_m(W)_?) approximates:
marg.out_W_j pr(q_a0b0|W_j).pr(W_j)
*/

            sumX += _X;
            sumX2 += (_X) * (_X);
            _EX[i] = sumX / (double) (i + 1);   //E[(W_1, ..., W_n)/n]
            _EX2[i] = sumX2 / (double) (i + 1);   //E[{(W_1, ..., W_n)/n}^2]

            //just for visualization:
            if (i % 1000 == 0) {
                System.out.println("*\t" + i + " samples already taken");
            }

            //todo I was here........
        }

        switch (statType) {
            case MEAN:
                return new OneDimFunction() {
                    @Override
                    public double eval(double v) {
                        int i = (int) Math.round(v); //to convert to int.
                        if (i < 0 || i > numSamples - 1) return 0;

                        return _EX[i];
                    }
                };
            case STD_DEV:
                return new OneDimFunction() {
                    @Override
                    public double eval(double v) {
                        int i = (int) Math.round(v); //to convert to int.
                        if (i < 0 || i > numSamples - 1) return 0;

                        return Math.sqrt(_EX2[i] - _EX[i] * _EX[i]);    //sqrt(E[X^2] - E[X]^2)
                    }
                };
            case STD_ERR:
                return new OneDimFunction() {
                    @Override
                    public double eval(double v) {
                        int i = (int) Math.round(v); //to convert to int.
                        if (i < 0 || i > numSamples - 1) return 0;

                        return Math.sqrt(_EX2[i] - _EX[i] * _EX[i]) / Math.sqrt((double) i + 1);    //sqrt(E[X^2] - E[X]^2)
                    }
                };
            default:
                throw new RuntimeException("unsupported " + statType);
        }
    } //end switch


    public static String fixedLengthStr(String string, int length) {
        return String.format("%1$" + length + "s", string);
    }
}
