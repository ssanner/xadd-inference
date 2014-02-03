package hgm.reports;

import hgm.asve.Pair;
import hgm.preference.Choice;
import hgm.preference.Preference;
import hgm.preference.PreferenceLearning;
import hgm.preference.db.DummyFeasiblePreferenceDatabase;
import hgm.preference.db.PartialPreferenceDatabase;
import hgm.preference.db.PreferenceDatabase;
import hgm.preference.predict.PolytopePrefLearningPredictor;
import hgm.preference.predict.PolytopePrefLearningPredictorUsingGibbs;
import hgm.preference.predict.PreferenceLearningPredictor;
import hgm.preference.predict.TrueSkillPrefLearningPredictor;
import hgm.sampling.MetropolisHastingsSampler;
import hgm.sampling.RejectionSampler;
import hgm.sampling.Sampler;
import hgm.sampling.VarAssignment;
import org.junit.Test;
import xadd.XADD;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 30/01/14
 * Time: 12:58 AM
 */
public class ReportTrueSkillAndPolyOnSynthesizedDB {
    @Test
    public void test1() {

    }

    public static void main(String[] args) {
//        ReportGibbsSamplerWithCDFsPerSampleForPreferenceLearning instance = new ReportGibbsSamplerWithCDFsPerSampleForPreferenceLearning();
//        instance.basicTest();
//        instance.dummyFeasibleTest();

    }

    @Test
    public void predictionTestOnDummyFeasibleModel() {
        double indicatorNoise = 0.0;
        boolean reduceLP = true;
        double relativeLeafValueBelowWhichRegionsAreTrimmed = -0.01;
        int numberOfSamples = 1000;

        System.out.println("#Samples:" + numberOfSamples + "\t relLeafThreshold:" + relativeLeafValueBelowWhichRegionsAreTrimmed + "\t indicator noise:" + indicatorNoise);

        List<Pair<String, PreferenceLearningPredictor>> predictors = new ArrayList<Pair<String, PreferenceLearningPredictor>>(Arrays.asList(
                new Pair<String, PreferenceLearningPredictor>("poly", new PolytopePrefLearningPredictorUsingGibbs(indicatorNoise, reduceLP, numberOfSamples, relativeLeafValueBelowWhichRegionsAreTrimmed, 0)),
                new Pair<String, PreferenceLearningPredictor>("true.skill", new TrueSkillPrefLearningPredictor(0))
        ));

        preferencePredictionOnDummyFeasibleModel(predictors);
    }

    @Test
    public void predictionTestOnDummyFeasibleModelUsingMetropolisAndProjection() {
        double indicatorNoise = 0.0;
        boolean reduceLP = true;
        double relativeLeafValueBelowWhichRegionsAreTrimmed = -0.01;
        int numberOfSamples = 1000;

        System.out.println("#Samples:" + numberOfSamples + "\t relLeafThreshold:" + relativeLeafValueBelowWhichRegionsAreTrimmed + "\t indicator noise:" + indicatorNoise);

        List<Pair<String, PreferenceLearningPredictor>> predictors = new ArrayList<Pair<String, PreferenceLearningPredictor>>(Arrays.asList(
                new Pair<String, PreferenceLearningPredictor>("metro", new PolytopePrefLearningPredictor(
                        indicatorNoise, reduceLP, numberOfSamples, relativeLeafValueBelowWhichRegionsAreTrimmed, 0) {
                    @Override
                    public Sampler makeNewSampler(XADD context, XADD.XADDNode posterior, VarAssignment initAssignment) {
                        return new MetropolisHastingsSampler(context, posterior, initAssignment);
                    }
                }),
                new Pair<String, PreferenceLearningPredictor>("rej", new PolytopePrefLearningPredictor(
                        indicatorNoise, reduceLP, numberOfSamples, relativeLeafValueBelowWhichRegionsAreTrimmed, 0) {
                    @Override
                    public Sampler makeNewSampler(XADD context, XADD.XADDNode posterior, VarAssignment initAssignment) {
                        return new RejectionSampler(context, posterior, initAssignment, 1);
                    }
                }
                )));

        preferencePredictionOnDummyFeasibleModel(predictors);
    }

    //#Samples vs Loss
    @Test
    public void roleOfSampleNumInPolytopePrefPredictionTestOnDummyFeasibleModelAndGibbs() {
        double indicatorNoise = 0.0;
        boolean reduceLP = true;
        double relativeLeafValueBelowWhichRegionsAreTrimmed = -0.01;
        int burnedSamples = 100;
        int maxNumberOfSamples = 10000 + burnedSamples;
        int[] sampleNums = new int[]{1, 10, 50, 100, 200, 500, 1000, 2000, 4000, 6000, 8000, 10000};

        System.out.println("#BurnedSamples=" + burnedSamples + "\t#samples:" + Arrays.toString(sampleNums) + "\t relLeafThreshold:" + relativeLeafValueBelowWhichRegionsAreTrimmed + "\t indicator noise:" + indicatorNoise);

        testingTheEffectOfNumberOfSamplesInPreferencePredictionOnDummyFeasibleModel(burnedSamples,
                new PolytopePrefLearningPredictorUsingGibbs(indicatorNoise, reduceLP, maxNumberOfSamples, relativeLeafValueBelowWhichRegionsAreTrimmed, 0), sampleNums);
    }

    /**
     * *******************************
     */
    public static void preferencePredictionOnDummyFeasibleModel(List<Pair<String /*name*/, PreferenceLearningPredictor>> predictors) {
        System.out.println(" #constraints and/or #dims vs. Loss");
        int numberOfItems = 500; // shouldn't have any significant effect (?) unless if its too small, dummy items will be repeated...
        int minDim = 10;
        int maxDim = 10;
        int minNumConstraints = 1;
        int maxNumConstraints = 60;
        int numberOfTestComparisonsPerDatabase = 60000;
        int numRepeatingEachExperiment = 10;

        for (int numDims = minDim; numDims <= maxDim; numDims++) {

            for (int numConstraints = minNumConstraints; numConstraints <= maxNumConstraints; numConstraints++) {

                Map<String, Double> predictor2averageLossMap = new HashMap<String, Double>();
                for (Pair<String, PreferenceLearningPredictor> predictor : predictors) {
                    predictor2averageLossMap.put(predictor.getFirstEntry(), 0d);
                }
                for (int experimentRepetitionCounter = 0; experimentRepetitionCounter < numRepeatingEachExperiment; experimentRepetitionCounter++) {

                    PreferenceDatabase completeDatabase =
                            new DummyFeasiblePreferenceDatabase(-PreferenceLearning.C, PreferenceLearning.C, 0d, 5d,
                                    numConstraints + numberOfTestComparisonsPerDatabase /*more preferences used for testing*/,
                                    numDims, numberOfItems /*number of items*/);

                    PreferenceDatabase trainingDb = new PartialPreferenceDatabase(completeDatabase, numConstraints);

                    for (Pair<String, PreferenceLearningPredictor> strPredictor : predictors) { //for different predictors same date is used for fair ness
                        String predName = strPredictor.getFirstEntry();
                        PreferenceLearningPredictor predictor = strPredictor.getSecondEntry();

                        List<Pair<String, Double>> info = predictor.learnToPredict(trainingDb);

                        long time5testingStart = System.currentTimeMillis();

                        int numLosses = 0;
                        for (int testCounter = 0; testCounter < numberOfTestComparisonsPerDatabase; testCounter++) {

                            Preference testPref = completeDatabase.getPreferenceResponses().get(numConstraints + testCounter);
                            Integer aId = testPref.getItemId1();
                            Integer bId = testPref.getItemId2();
                            Double[] a = completeDatabase.getItemAttributeValues(aId);
                            Double[] b = completeDatabase.getItemAttributeValues(bId);

                            Choice predictedChoice = predictor.predictPreferenceChoice(a, b);


                            if (!predictedChoice.equals(testPref.getPreferenceChoice())) {
                                numLosses++;

                            }

                        }//end test for

                        double averageLoss = numLosses / (double) numberOfTestComparisonsPerDatabase;
                        System.out.print("[" + predName + ":]\t averageLoss(" + numDims + ":" + numConstraints + ") = " + averageLoss + "\t\t");

                        predictor2averageLossMap.put(predName, predictor2averageLossMap.get(predName) + (averageLoss / (double) numRepeatingEachExperiment));

                        long time6testingEnd = System.currentTimeMillis();

                    /*// #Dims \t\t #Constraints \t\t time for 1. Posterior calc \t\t posterior nodes \t\t Elapsed time for 3. w_i - CDF calculated: \t\t Elapsed time for 4. sampling:
                    System.out.println("[" + predName + ":]\t" +	numDims + " \t\t " + numConstraints + "\t\t\t" + info +
                            //(time2posteriorCalculated - time1start) + "\t\t (" + posteriorNodeCount +
//                        ")\t\t" + //(time4samplerInitialized - time3posteriorReduced) +
//                        "\t\t " + (time5samplesTaken - time3posteriorReduced) +
                            "\t\t" + (time6testingEnd - time5testingStart));*/
                    }//end predictor loop
                    System.out.print("\n");
                }//end repetition loop
                System.out.println("(#DIM:" + numDims + "/CNST:" + numConstraints + ") >>>>>>>>> predictor2AverageLossMap = " + predictor2averageLossMap);
            } // end numConstraints for
        } // end numDim for

    }

    //#samples vs. loss
    public static void testingTheEffectOfNumberOfSamplesInPreferencePredictionOnDummyFeasibleModel(
            int burnedSamples,
            PolytopePrefLearningPredictor predictor, int[] numSamples) {
        System.out.println("SAMPLES vs LOSS");

        int numberOfItems = 500; // shouldn't have any significant effect (?) unless if its too small, dummy items will be repeated...
        int minDim = 10;
        int maxDim = 10;
        int[] numConstraintsArray = new int[]{/*2, 4, 8, 12, 16, 24, */36, 48};
//        int minNumConstraints = 16;
//        int maxNumConstraints = 40;
        int numberOfTestComparisonsPerDatabase = 60000;

//        int maxSampleNum = numSamples[0];
//        for (int sampleNum : numSamples) {
//            maxSampleNum = Math.max(maxSampleNum, sampleNum);
//        }


        for (int numDims = minDim; numDims <= maxDim; numDims++) {
            for (int numConstraints : numConstraintsArray) {

                PreferenceDatabase completeDatabase =
                        new DummyFeasiblePreferenceDatabase(-PreferenceLearning.C, PreferenceLearning.C, 0d, 5d,
                                numConstraints + numberOfTestComparisonsPerDatabase /*more preferences used for testing*/,
                                numDims, numberOfItems /*number of items*/);

                PreferenceDatabase trainingDb = new PartialPreferenceDatabase(completeDatabase, numConstraints);


                List<Pair<String, Double>> info = predictor.learnToPredict(trainingDb);
                System.out.println("Learning completed: " + info);


                Map<Integer, Double> sampleCountToLossMap = new HashMap<Integer, Double>();

                for (int numSample : numSamples) {

                    int numLosses = 0;
                    for (int testCounter = 0; testCounter < numberOfTestComparisonsPerDatabase; testCounter++) {

                        Preference testPref = completeDatabase.getPreferenceResponses().get(numConstraints + testCounter);
                        Integer aId = testPref.getItemId1();
                        Integer bId = testPref.getItemId2();
                        Double[] a = completeDatabase.getItemAttributeValues(aId);
                        Double[] b = completeDatabase.getItemAttributeValues(bId);

                        Choice predictedChoice = predictor.predictPreferenceChoice(a, b, burnedSamples, numSample);


                        if (!predictedChoice.equals(testPref.getPreferenceChoice())) {
                            numLosses++;

                        }

                    }//end predictor loop

                    double averageLoss = numLosses / (double) numberOfTestComparisonsPerDatabase;
                    sampleCountToLossMap.put(numSample, averageLoss);

                }//end sample loop

                System.out.print("dim/constr(" + numDims + ":" + numConstraints + ") \t " + sampleCountToLossMap);
                System.out.print("\n");
            } // end numConstraints for
        } // end numDim for

    }


    //TODO
    //TODO
    /*private void sample2DToMatlabFile(String var1, String var2, int numSamples, Sampler sampler) throws FileNotFoundException {
        PrintStream ps;
        ps = new PrintStream(new FileOutputStream(SCATTER_2D_FILENAME));

        for (int i = 0; i < numSamples; i++) {
            VarAssignment assign = sampler.sample();
            double x1 = assign.getContinuousVar(var1);
            double x2 = assign.getContinuousVar(var2);
            ps.println(x1 + "\t" + x2);
        }

        ps.close();

    }*/

}
