package hgm.poly.pref.reports;

import hgm.asve.Pair;
import hgm.poly.pref.PolyPreferenceLearningPredictor;
import hgm.poly.pref.PolyPreferenceLearningPredictorUsingGibbsSampler;
import hgm.poly.pref.reports.db.SyntheticDistributionUtils;
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
import hgm.sampling.MetropolisHastingsSampler;
import hgm.sampling.RejectionSampler;
import hgm.sampling.VarAssignment;
import hgm.sampling.XaddSampler;
import hgm.utils.Utils;
import org.junit.Test;
import xadd.XADD;

import java.io.IOException;
import java.util.*;

/**
 * Written before UAI 2014
 */
//todo complete this file..............
public class ReportPolyGibbsTrueSkillMHRejectionOnSynthesizedDB {

    @Test
    public void test1() {

    }

    public static void main(String[] args) throws IOException {
        // ReportGibbsSamplerWithCDFsPerSampleForPreferenceLearning instance = new ReportGibbsSamplerWithCDFsPerSampleForPreferenceLearning();
        // instance.basicTest();
        // instance.dummyFeasibleTest();

        // System.out.println(System.getProperty("java.library.path"));

        ReportPolyGibbsTrueSkillMHRejectionOnSynthesizedDB instance = new ReportPolyGibbsTrueSkillMHRejectionOnSynthesizedDB();
        instance.predictionTestOnDummyFeasibleModelUsingPolyXadPolyMetropolisAndProjection();
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
                                    new PolyPreferenceLearningPredictorUsingGibbsSampler(indicatorNoise,
                                            numberOfSamples, burnedSamples, maxGateConstraintViolation, -PreferenceLearning.C, PreferenceLearning.C))
                            ,new Pair<String, PreferenceLearningPredictor>("xadd.poly",
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
    @Test
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
    }

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

    // #samples vs. loss
    public static void testingTheEffectOfNumberOfSamplesInPreferencePredictionOnDummyFeasibleModel(int burnedSamples,
                                                                                                   XaddPolytopePrefLearningPredictor predictor, int[] numSamples) {
        System.out.println("SAMPLES vs LOSS");

        int numberOfItems = 500; // shouldn't have any significant effect (?) unless if its too small, dummy items will be repeated...
        int minDim = 10;
        int maxDim = 10;
        int[] numConstraintsArray = new int[]{/* 2, 4, 8, 12, 16, 24, */36, 48};
        // int minNumConstraints = 16;
        // int maxNumConstraints = 40;
        int numberOfTestComparisonsPerDatabase = 60000;

        // int maxSampleNum = numSamples[0];
        // for (int sampleNum : numSamples) {
        // maxSampleNum = Math.max(maxSampleNum, sampleNum);
        // }

        for (int numDims = minDim; numDims <= maxDim; numDims++) {
            for (int numConstraints : numConstraintsArray) {

                PreferenceDatabase completeDatabase = new DummyFeasiblePreferenceDatabase(
                        -PreferenceLearning.C,
                        PreferenceLearning.C,
                        0d,
                        5d,
                        numConstraints + numberOfTestComparisonsPerDatabase /* more preferences used for testing */,
                        numDims, numberOfItems /* number of items */);

                PreferenceDatabase trainingDb = new PartialPreferenceDatabase(completeDatabase, numConstraints);

                List<Pair<String, Double>> info = predictor.learnToPredict(trainingDb);
                System.out.println("Learning completed: " + info);

                Map<Integer, Double> sampleCountToLossMap = new HashMap<Integer, Double>();

                for (int numSample : numSamples) {

                    int numLosses = 0;
                    for (int testCounter = 0; testCounter < numberOfTestComparisonsPerDatabase; testCounter++) {

                        Preference testPref = completeDatabase.getPreferenceResponses().get(numConstraints
                                + testCounter);
                        Integer aId = testPref.getItemId1();
                        Integer bId = testPref.getItemId2();
                        Double[] a = completeDatabase.getItemAttributeValues(aId);
                        Double[] b = completeDatabase.getItemAttributeValues(bId);

                        Choice predictedChoice = predictor.predictPreferenceChoice(a, b, burnedSamples, numSample);

                        if (!predictedChoice.equals(testPref.getPreferenceChoice())) {
                            numLosses++;

                        }

                    }// end predictor loop

                    double averageLoss = numLosses / (double) numberOfTestComparisonsPerDatabase;
                    sampleCountToLossMap.put(numSample, averageLoss);

                }// end sample loop

                System.out.print("dim/constr(" + numDims + ":" + numConstraints + ") \t " + sampleCountToLossMap);
                System.out.print("\n");
            } // end numConstraints for
        } // end numDim for

    }

    // TODO
    // TODO
    /* private void sample2DToMatlabFile(String var1, String var2, int numSamples, Sampler sampler) throws FileNotFoundException { PrintStream ps; ps
     * = new PrintStream(new FileOutputStream(SCATTER_2D_FILENAME));
	 * 
	 * for (int i = 0; i < numSamples; i++) { VarAssignment assign = sampler.sample(); double x1 = assign.getContinuousVar(var1); double x2 =
	 * assign.getContinuousVar(var2); ps.println(x1 + "\t" + x2); }
	 * 
	 * ps.close();
	 * 
	 * } */


    public static String fixedLengthStr(String string, int length) {
        return String.format("%1$"+length+ "s", string);
    }
}
