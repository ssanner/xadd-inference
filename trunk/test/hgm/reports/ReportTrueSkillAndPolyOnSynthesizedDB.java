package hgm.reports;

import hgm.asve.Pair;
import hgm.preference.Choice;
import hgm.preference.Preference;
import hgm.preference.PreferenceLearning;
import hgm.preference.db.DummyFeasiblePreferenceDatabase;
import hgm.preference.db.PartialPreferenceDatabase;
import hgm.preference.db.PreferenceDatabase;
import hgm.preference.predict.PolytopePrefLearningPredictor;
import hgm.preference.predict.PreferenceLearningPredictor;
import hgm.preference.predict.TrueSkillPrefLearningPredictor;
import org.junit.Test;

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
    public void PolytopeTestOnDummyFeasibleModel() {
        double indicatorNoise = 0.0;
        boolean reduceLP = true;
        double relativeLeafValueBelowWhichRegionsAreTrimmed = -0.0001;
        int numberOfSamples = 1000;

        List<Pair<String, PreferenceLearningPredictor>> predictors = new ArrayList<Pair<String, PreferenceLearningPredictor>>(Arrays.asList(
//                new Pair<String, PreferenceLearningPredictor>("poly", new PolytopePrefLearningPredictor(indicatorNoise, reduceLP, numberOfSamples, relativeLeafValueBelowWhichRegionsAreTrimmed, 0)),
                new Pair<String, PreferenceLearningPredictor>("true.skill", new TrueSkillPrefLearningPredictor(0))
        ));

        preferencePredictionOnDummyFeasibleModel(predictors);
    }

    public static void preferencePredictionOnDummyFeasibleModel(List<Pair<String /*name*/, PreferenceLearningPredictor>> predictors) {
        int numberOfItems = 500; // shouldn't have any significant effect (?) unless if its too small, dummy items will be repeated...
        int minDim = 2;
        int maxDim = 52;
        int minNumConstraints = 1;
        int maxNumConstraints = 30;


        int numberOfTestComparisonsPerDatabase = 1000;

        for (int numDims = minDim; numDims <= maxDim; numDims++) {
            for (int numConstraints = minNumConstraints; numConstraints <= maxNumConstraints; numConstraints++) {

                PreferenceDatabase completeDatabase =
                        new DummyFeasiblePreferenceDatabase(-PreferenceLearning.C, PreferenceLearning.C, 0d, 5d,
                                numConstraints + numberOfTestComparisonsPerDatabase /*more preferences used for testing*/,
                                numDims, numberOfItems /*number of items*/);


                PreferenceDatabase trainingDb = new PartialPreferenceDatabase(completeDatabase, numConstraints);

                for (Pair<String, PreferenceLearningPredictor> strPredictor : predictors) {
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


                    long time6testingEnd = System.currentTimeMillis();

                    /*// #Dims \t\t #Constraints \t\t time for 1. Posterior calc \t\t posterior nodes \t\t Elapsed time for 3. w_i - CDF calculated: \t\t Elapsed time for 4. sampling:
                    System.out.println("[" + predName + ":]\t" +	numDims + " \t\t " + numConstraints + "\t\t\t" + info +
                            //(time2posteriorCalculated - time1start) + "\t\t (" + posteriorNodeCount +
//                        ")\t\t" + //(time4samplerInitialized - time3posteriorReduced) +
//                        "\t\t " + (time5samplesTaken - time3posteriorReduced) +
                            "\t\t" + (time6testingEnd - time5testingStart));*/
                }//end predictor loop
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
