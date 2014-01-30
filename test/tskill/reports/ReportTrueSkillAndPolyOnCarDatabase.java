package tskill.reports;

import hgm.preference.Preference;
import hgm.preference.PreferenceLearning;
import hgm.preference.db.DiscretePreferenceDatabase;
import hgm.preference.db.PartialPreferenceDatabase;
import hgm.preference.db.PreferenceDatabase;
import hgm.preference.db.ShuffledPreferenceDatabase;
import hgm.preference.db.car.CarPreferenceDatabase;
import hgm.sampling.Sampler;
import hgm.sampling.VarAssignment;
import hgm.sampling.gibbs.GibbsSamplerWithCDFsPerSample;
import tskill.jskills.trueskill.TwoTeamTrueSkillCalculator;
import junit.framework.Assert;
import org.junit.Test;
import tskill.ranking.TrueSkillModel;
import xadd.XADD;
import tskill.jskills.*;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 26/01/14
 * Time: 8:09 PM
 */
public class ReportTrueSkillAndPolyOnCarDatabase {


    //*********************************************************************************************

    @Test
    public void trueSkillOnCarDatabaseTest() {
        SkillCalculator calculator = new
//                FactorGraphTrueSkillCalculator();
                TwoTeamTrueSkillCalculator();


        Set<Integer> adviserIds = new HashSet<Integer>();
        for (int id = 0; id < 1; id++) {   //todo Only the first advice!?
            adviserIds.add(id);
        }

        DiscretePreferenceDatabase carDB = new ShuffledPreferenceDatabase(CarPreferenceDatabase.fetchCarPreferenceDataBase1stExperiment(adviserIds));
        Integer attribCount = carDB.getNumberOfAttributes();
        System.out.println("carDB.getNumberOfAttributes() = " + attribCount);

        List<Preference> preferenceResponses = carDB.getPreferenceResponses();
        System.out.println("preferenceResponses = " + preferenceResponses);
        System.out.println("preferenceResponses.size() = " + preferenceResponses.size());

        for (int attribId = 0; attribId < attribCount; attribId++) {
            System.out.println("carDB.getAttribChoices(" + attribId + ") = " + carDB.getAttribChoices(attribId));
        }

        TrueSkillModel<Integer, Double> model = new TrueSkillModel<Integer, Double>();
        //1. creating players:
        for (Integer attribId = 0; attribId < attribCount; attribId++) {
            Collection<Double> attribChoicesCol = carDB.getAttribChoices(attribId);
            Double[] attribChoicesArray = attribChoicesCol.toArray(new Double[attribChoicesCol.size()]);
            model.put(attribId, attribChoicesArray);
        }

        GameInfo gameInfo = GameInfo.getDefaultGameInfo(); //todo: I should use reasonable default parameters of my own...

        //2. training phase: (apparently not needed)
        Integer NUM_TRAINING = 0;
        /*List<Preference> chosenTrainingPrefs = preferenceResponses.subList(0, NUM_TRAINING);  //NOTE: given the first 10 prefs, what are other prefs?
        for (Preference trainPref : chosenTrainingPrefs) {
            Map<IPlayer, Rating> newRatings = calculateNewRankingsGiven(trainPref, carDB, model, calculator, gameInfo);
            model.updateRatings(newRatings);
        }*/

        //3. test & update:
        List<Preference> chosenTestPrefs = preferenceResponses.subList(NUM_TRAINING, preferenceResponses.size());
        int numComparisons = 0;
        int numLosses = 0;
        for (Preference testPref : chosenTestPrefs) {
            numComparisons++;
            Preference.Choice predictedChoice = TrueSkillUtils.predictPreferenceChoice(testPref.getItemId1(), testPref.getItemId2(), carDB, model, 0 /*epsilon*/);
//            System.out.println("prediction = " + predictedChoice + " .. testPref = " + testPref.getPreferenceChoice());
            if (!predictedChoice.equals(testPref.getPreferenceChoice())) {
                numLosses++;
            }

            //Update:  //todo should I?
            Map<IPlayer, Rating> newRatings = TrueSkillUtils.calculateNewRankingsGiven(testPref, carDB, model, calculator, gameInfo);
            model.updateRatings(newRatings);

            double averageLoss = numLosses / (double) numComparisons;
//            System.out.println("averageLoss(" + numComparisons + ") = " + averageLoss);
            System.out.println((numComparisons-1) + "\t" + averageLoss);
        }

    }


    //***********************************************************************************************//

    @Test
    public void testGibbsSamplingAndPolytopesOnCarDatabase() {
//        int numberOfItems = 200; // shouldn't have any significant effect (?) unless if its too small, dummy items will be repeated...
//        int minDim = 4;
//        int maxDim = 12;

        //number of constraints = number of known preferences
        int minNumConstraints = 0;

        double indicatorNoise = 0.45;
        boolean reduceLP = true;
        int numberOfSamplesTakenFromPosterior = 500;
        double relativeLeafValueBelowWhichRegionsAreTrimmed = -100d;//0.00000000001;

        System.out.println("indicatorNoise = " + indicatorNoise + "\t#Samples = " + numberOfSamplesTakenFromPosterior + "\tLeaf trim = " + relativeLeafValueBelowWhichRegionsAreTrimmed);



        Set<Integer> adviserIds = new HashSet<Integer>();
        adviserIds.add(0); //only the first guy....
        DiscretePreferenceDatabase completeDatabase = //new BinarizedPreferenceDatabase(
                new ShuffledPreferenceDatabase(CarPreferenceDatabase.fetchCarPreferenceDataBase1stExperiment(adviserIds));
        int numDims = completeDatabase.getNumberOfAttributes();
        List<Preference> preferenceResponses = completeDatabase.getPreferenceResponses();
        int maxNumConstraints = preferenceResponses.size();


        int numLosses = 0;
        int numComparisons = 0;
        for (int numConstraints = minNumConstraints; numConstraints < maxNumConstraints; numConstraints++) {
            numComparisons++;

            /**
             * Partial database only contains the first 'numConstraints' elements of the database....
             */
            PreferenceDatabase partialDB = new PartialPreferenceDatabase(completeDatabase, numConstraints);

            XADD context = new XADD();
            PreferenceLearning learning = new PreferenceLearning(context, partialDB, indicatorNoise, "w");
            PreferenceLearning.EPSILON = 0.0; //since in the car DB there is no equality...

            long time1start = System.currentTimeMillis();
            // Pr(W | R^{n+1})
            XADD.XADDNode posterior = learning.computePosteriorWeightVector(reduceLP, relativeLeafValueBelowWhichRegionsAreTrimmed);
            int posteriorNodeCount = posterior.collectNodes().size();
            long time2posteriorCalculated = System.currentTimeMillis();


            //This should only be used for visualization (???), not in sampling ?
            fixVarLimits(context, posterior, -10d, 10d); //todo what should I do???? COMPARE WITH THE TRUE-SKILL X X X X

            long time3posteriorReduced = System.currentTimeMillis();


            Sampler sampler = new GibbsSamplerWithCDFsPerSample(context, posterior, learning.generateAWeightVectorHighlyProbablePosteriorly());
//                long time4samplerInitialized = System.currentTimeMillis();

            // now that the first 'numConstraints' preferences are used to make the posterior W we calculate the probability of the next preference....
            Preference nextPref = completeDatabase.getPreferenceResponses().get(numConstraints + 1);
            Integer aId = nextPref.getItemId1();
            Integer bId = nextPref.getItemId2();
            Double[] a = completeDatabase.getItemAttributeValues(aId);
            Double[] b = completeDatabase.getItemAttributeValues(bId);
            int timesAIsGreaterThanB = 0;
            int timesBIsGreaterThanA = 0;
            int timesAEqualsB = 0;
            int EPSILON = 0; //since in the data set we do not have (=)

            //todo ...

            for (int i = 0; i < numberOfSamplesTakenFromPosterior; i++) {
                VarAssignment assign = sampler.sample();
//                System.out.println("assign = " + assign);
                Double eval = context.evaluate(context._hmNode2Int.get(posterior), assign.getBooleanVarAssign(), assign.getContinuousVarAssign());
                if (eval == null || eval <= 0.0) System.out.println("eval = " + eval);
                Assert.assertTrue(eval > 0.0);
                Double[] ws = assign.getContinuousVarAssignAsArray();
//                System.out.println("--------------------------");
//                System.out.println("ws = " + Arrays.toString(ws));
//                System.out.println("a = " + Arrays.toString(a));
//                System.out.println("b = " + Arrays.toString(b));
                double utilA = util(a, ws);
                double utilB = util(b, ws);
//                System.out.println("utilA = " + utilA + "\tutilB = " + utilB);

                if (utilA - utilB > EPSILON) {
                    timesAIsGreaterThanB++;
                } else if (utilB - utilA > EPSILON) {
                    timesBIsGreaterThanA++;
                } else timesAEqualsB++;
            }

//            System.out.println("timesAEqualsB = " + timesAEqualsB);
//            System.out.println("timesAIsGreaterThanB = " + timesAIsGreaterThanB);
//            System.out.println("timesBIsGreaterThanA = " + timesBIsGreaterThanA);
            double maxCount = Math.max(timesAEqualsB, Math.max(timesAIsGreaterThanB, timesBIsGreaterThanA));
            Preference.Choice predictedChoice;
            if (maxCount == timesAIsGreaterThanB) predictedChoice = Preference.Choice.FIRST;
            else if (maxCount == timesBIsGreaterThanA) predictedChoice = Preference.Choice.SECOND;
            else predictedChoice = Preference.Choice.EQUAL;

//            System.out.println("nextPref.getPreferenceChoice() = " + nextPref.getPreferenceChoice());
//            System.out.println("predictedChoice = " + predictedChoice);
            if (!predictedChoice.equals(nextPref.getPreferenceChoice())) {
                numLosses++;
            }

            double averageLoss = numLosses / (double) numComparisons;
            System.out.println("averageLoss(" + (numComparisons-1) + ") = " + averageLoss);

            long time5samplesTaken = System.currentTimeMillis();

            // #Dims \t\t #Constraints \t\t time for 1. Posterior calc \t\t posterior nodes \t\t Elapsed time for 3. w_i - CDF calculated: \t\t Elapsed time for 4. sampling:
           /* System.out.println(numDims + " \t\t " + numConstraints + "\t\t\t" + (time2posteriorCalculated - time1start) + "\t\t (" + posteriorNodeCount +
                    ")\t\t" + //(time4samplerInitialized - time3posteriorReduced) +
                    "\t\t " + (time5samplesTaken - time3posteriorReduced));*/


        } // end numConstraints for


    }

    //sum_i a_i.b_i
    private double util(Double[] a, Double[] b) {
        double u = 0d;
        if (a.length != b.length) throw new RuntimeException("size mismatch");
        for (int i = 0; i < a.length; i++) {
            u += a[i] * b[i];
        }
        return u;
    }

    private void fixVarLimits(XADD context, XADD.XADDNode root, double varMin, double varMax) {
        HashSet<String> vars = root.collectVars();
        for (String var : vars) {
            context._hmMinVal.put(var, varMin);
            context._hmMaxVal.put(var, varMax);
        }
    }

}
