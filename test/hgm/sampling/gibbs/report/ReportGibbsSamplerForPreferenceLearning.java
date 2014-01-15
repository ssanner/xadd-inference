package hgm.sampling.gibbs.report;

import hgm.preference.PreferenceLearning;
import hgm.preference.PreferenceLearningTest;
import hgm.preference.Preference;
import hgm.preference.db.DummyFeasiblePreferenceDatabase;
import hgm.preference.db.PreferenceDatabase;
import hgm.sampling.Sampler;
import hgm.sampling.SamplingFailureException;
import hgm.sampling.VarAssignment;
import hgm.sampling.gibbs.GibbsSampler;
import hgm.utils.vis.XaddVisualizer;
import junit.framework.Assert;
import org.junit.Test;
import xadd.XADD;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 11/01/14
 * Time: 10:42 AM
 */
public class ReportGibbsSamplerForPreferenceLearning {

    public static void main(String[] args) {
        ReportGibbsSamplerForPreferenceLearning instance = new ReportGibbsSamplerForPreferenceLearning();
//        instance.basicTest();
//        instance.dummyTest();
//        instance.dummyFeasibleTest();
//        instance.debugDummyTest();
        instance.effectOfLPReduce();
    }

    PreferenceDatabase testDB1 = new PreferenceDatabase() {
        Preference[] prefs = new Preference[]{
                new Preference(1, 2, Preference.Choice.FIRST),
                new Preference(1, 3, Preference.Choice.FIRST),
//                new Preference(2, 3, Preference.Choice.FIRST),
        };

        List<Double[]> items = new ArrayList<Double[]>(5);

        {
            items.add(new Double[]{0.0, 1.0});
            items.add(new Double[]{25.0, 4.5});
            items.add(new Double[]{30.0, 9.0});
            items.add(new Double[]{30.0, 16.0});
            items.add(new Double[]{40.0, 25.0});
        }

        @Override
        public int getNumberOfAttributes() {
            return items.get(0).length;
        }

        @Override
        public int numberOfItems() {
            return items.size();
        }

        @Override
        public List<Preference> getPreferenceResponses() {
            return Arrays.asList(prefs);
        }

        @Override
        public Double[] getItemAttributeValues(int itemId) {
            return items.get(itemId);
        }

        @Override
        public double[] getAuxiliaryWeightVector() {
            return null;
        }
    }; //end inner class.

    @Test
    public void basicTest() {
        XADD context = new XADD();
        PreferenceLearning learning = new PreferenceLearning(context, testDB1, 0.2, "w");
//        PreferenceLearning learning = new PreferenceLearning(context, new DummyPreferenceDatabase(-PreferenceLearning.C, PreferenceLearning.C, 0, 5, 5, 2, 120), 0.2, false);

        // Pr(W | R^{n+1})
        XADD.XADDNode utilityWeights = learning.computePosteriorWeightVector(false);
        fixVarLimits(context, utilityWeights, -30d, 60d);

        context.getGraph(context._hmNode2Int.get(utilityWeights)).launchViewer("test");
        XaddVisualizer.visualize(utilityWeights, "test", context);


        //now I sample from it:
        Sampler sampler = new GibbsSampler(context, utilityWeights);
        for (int i = 0; i < 50; i++) {
            VarAssignment assign = sampler.sample();
            System.out.println("t = " + assign);
        }

    }

    @Deprecated
    @Test
    public void debugDummyTest() {
        int numberOfItems = 200; // shouldn't have any significant effect (?) unless if its too small, dummy items will be repeated...

        int maxDummyDataBaseGenerationTrials = 1; //max times tries to make dummy preference DBs that are consistent.
        int numConstraints = 28;
        int numDims = 4;

        PreferenceDatabase db = PreferenceLearningTest.generateDummyPreferenceDatabase(0, 5, numConstraints, numDims, numberOfItems /*number of items*/);

        XADD context = new XADD();
        PreferenceLearning learning = new PreferenceLearning(context, db, 0.0, "w");

        long time1 = System.currentTimeMillis();
        // Pr(W | R^{n+1})
        XADD.XADDNode utilityWeights = learning.computePosteriorWeightVector(false);
        System.out.println("----------------------------");
        System.out.println("utilityWeights: #nodes = (" + utilityWeights.collectNodes().size()
                + ")\t #Dim: " + numDims + " \t #Constraints: " + numConstraints);
        fixVarLimits(context, utilityWeights, -0.5d, 5.5d);

        /****
         if (numDims == 2 && numConstraints >= 5) {
         context.getGraph(context._hmNode2Int.get(utilityWeights)).launchViewer("W" + numDims+ "-" + numConstraints);
         XaddVisualizer.visualize(utilityWeights, "W" + numDims+ "-" + numConstraints, context);
         }
         */

        GibbsSampler sampler = new GibbsSampler(context, utilityWeights);


        try {
            VarAssignment assign = sampler.sample();
            System.out.println("Assign = " + assign);
        } catch (SamplingFailureException e) {
            System.err.println("e = " + e);
        }

        long time2 = System.currentTimeMillis();
        System.out.println("#Dim: " + numDims + " \t #Constraints: " + numConstraints + "\t Elapsed time: " + (time2 - time1));

    }

    @Test
    public void dummyTest() {
        int numberOfItems = 200; // shouldn't have any significant effect (?) unless if its too small, dummy items will be repeated...
        int maxDimCount = 16;
        int maxConstraintCount = 31;

        int maxDummyDataBaseGenerationTrials = 20; //max times tries to make dummy preference DBs that are consistent.
        int numberOfSamplesFromEachDatabase = 1;

        for (int numDims = 1; numDims <= maxDimCount; numDims++) {
            for (int numConstraints = 0; numConstraints < maxConstraintCount; numConstraints++) {

                int currentDatabaseGenerationAttempts = 0;
                boolean shouldGenerateAnotherDatabase;

                do {

                    PreferenceDatabase db = PreferenceLearningTest.generateDummyPreferenceDatabase(0, 5, numConstraints, numDims, numberOfItems /*number of items*/);
                    shouldGenerateAnotherDatabase = false; //since there is no reason the current DB is not good enough

                    XADD context = new XADD();
                    PreferenceLearning learning = new PreferenceLearning(context, db, 0.1, "w");

                    long time1 = System.currentTimeMillis();
                    // Pr(W | R^{n+1})
                    XADD.XADDNode utilityWeights = learning.computePosteriorWeightVector(false);
//                    System.out.println("----------------------------");
//                    System.out.println("utilityWeights: #nodes = (" + utilityWeights.collectNodes().size()
//                            + ")\t #Dim: " + numDims + " \t #Constraints: " + numConstraints);
                    fixVarLimits(context, utilityWeights, -0.5d, 5.5d);

                    /****
                     if (numDims == 2 && numConstraints >= 5) {
                     context.getGraph(context._hmNode2Int.get(utilityWeights)).launchViewer("W" + numDims+ "-" + numConstraints);
                     XaddVisualizer.visualize(utilityWeights, "W" + numDims+ "-" + numConstraints, context);
                     }
                     */

                    GibbsSampler sampler = new GibbsSampler(context, utilityWeights);


                    boolean sampleTakingWasSuccessful = true;
                    try {
                        for (int i = 0; i < numberOfSamplesFromEachDatabase; i++) {
                            VarAssignment assign = sampler.sample();
                            //todo: commented tempo...
//                            System.out.println("Assign = " + assign);
                        }
                    } catch (SamplingFailureException e) {
                        sampleTakingWasSuccessful = false;
//                        System.err.println("e = " + e);
                        if (++currentDatabaseGenerationAttempts < maxDummyDataBaseGenerationTrials) {
                            shouldGenerateAnotherDatabase = true;
                        } else {
//                            System.err.println("********************************************");
//                            System.err.println("* COULD NOT FIND A POSITIVE DISTRIBUTION FOR: " +
//                            "#Dim: " + numDims + " \t #Constraints: " + numConstraints);
                        }
                    }

                    long time2 = System.currentTimeMillis();
                    System.out.println("#Dim: " + numDims + " \t #Constraints: " + numConstraints + "\t Elapsed time: " + (time2 - time1) + "\t\t" +
                            (sampleTakingWasSuccessful ? "Y" : "-"));

                } while (shouldGenerateAnotherDatabase);

                //int maxExpUtilItem = learning.computeItemWithMaxExpectedUtilityVersion2(utilityWeights, "w", "x");
//            System.out.println("maxExpUtilItem = " + maxExpUtilItem);

            } // end numConstraints for
        } // end numDim for

    }

    @Test
    public void dummyFeasibleTest() {
        int numberOfItems = 200; // shouldn't have any significant effect (?) unless if its too small, dummy items will be repeated...
        int minDim = 1;
        int maxDim= 12;
        int minNumConstraints = 0;
       int maxNumConstraints = 10;
        double indicatorNoise = 0.1;
        boolean reduceLP = true;
        int numberOfSamplesFromEachDatabase = 10000;

        System.out.println("#Dims| #Constraints|  Time:Posterior|  #Nodes:posterior|Time:CDF| Time:sampling");
        for (int numDims = minDim; numDims <= maxDim; numDims++) {
            for (int numConstraints = minNumConstraints; numConstraints <= maxNumConstraints; numConstraints++) {

                PreferenceDatabase db =
                        new DummyFeasiblePreferenceDatabase(-PreferenceLearning.C, PreferenceLearning.C, 0, 5, numConstraints, numDims, numberOfItems /*number of items*/);

                XADD context = new XADD();
                PreferenceLearning learning = new PreferenceLearning(context, db, indicatorNoise, "w");

                long time1start = System.currentTimeMillis();
                // Pr(W | R^{n+1})
                XADD.XADDNode posterior = learning.computePosteriorWeightVector(reduceLP);
                int posteriorNodeCount = posterior.collectNodes().size();
                long time2posteriorCalculated = System.currentTimeMillis();


                //This should only be used for visualization (???), not in sampling ?
                fixVarLimits(context, posterior, -0.5d, 5.5d);

                /****
                 if (numDims == 2 && numConstraints >= 5) {
                 context.getGraph(context._hmNode2Int.get(utilityWeights)).launchViewer("W" + numDims+ "-" + numConstraints);
                 XaddVisualizer.visualize(utilityWeights, "W" + numDims+ "-" + numConstraints, context);
                 }
                 */

                //todo: posterior should be LP-reduced....
                long time3posteriorReduced = System.currentTimeMillis();

                GibbsSampler sampler = new GibbsSampler(context, posterior, learning.generateAWeightVectorHighlyProbablePosteriorly());
                long time4samplerInitialized = System.currentTimeMillis();

                for (int i = 0; i < numberOfSamplesFromEachDatabase; i++) {
                    VarAssignment assign = sampler.sample();
                    Double eval = context.evaluate(context._hmNode2Int.get(posterior), assign.getBooleanVarAssign(), assign.getContinuousVarAssign());
                    if (eval == null || eval<=0.0) System.out.println("eval = " + eval);
                    Assert.assertTrue(eval > 0.0);
                }
                long time5samplesTaken = System.currentTimeMillis();

                // #Dims \t\t #Constraints \t\t time for 1. Posterior calc \t\t posterior nodes \t\t Elapsed time for 3. w_i - CDF calculated: \t\t Elapsed time for 4. sampling:
                System.out.println(numDims + " \t\t " + numConstraints + "\t\t\t" + (time2posteriorCalculated - time1start) + "\t\t (" + posteriorNodeCount +
                        ")\t\t" + (time4samplerInitialized - time3posteriorReduced) +
                        "\t\t " + (time5samplesTaken - time4samplerInitialized));

/*
                System.out.println("#Dim: " + numDims + " \t #Constraints: " + numConstraints +
                        "\n\t Elapsed time for 1. Posterior calc:      \t\t" + (time2posteriorCalculated - time1start) + "\t\t posterior Nodes = " + posteriorNodeCount +
//                       "\n\t Elapsed time for 2. Posterior reduceLP:  \t\t" + (time3posteriorReduced - time2posteriorCalculated) +
                        "\n\t Elapsed time for 3. w_i - CDF calculated:\t\t" + (time4samplerInitialized - time3posteriorReduced) +
                        "\n\t Elapsed time for 4. sampling:            \t\t" + (time5samplesTaken - time4samplerInitialized));

*/

            } // end numConstraints for
        } // end numDim for

    }

    @Test
    public void effectOfLPReduce() {
        int numberOfItems = 200; // shouldn't have any significant effect (?) unless if its too small, dummy items will be repeated...
        int minDim = 1;
        int maxDim= 12;
        int minNumConstraints = 0;
        int maxNumConstraints = 40;
        double indicatorNoise = 0.1;
        boolean reduceLP = true;
        int numberOfSamplesFromEachDatabase = 10000;

        System.out.println("#Dims| #Constraints|  Time:Posterior |   Time:LP || #Nodes:-LP | #Nodes:+LP ");
        for (int numDims = minDim; numDims <= maxDim; numDims++) {
            for (int numConstraints = minNumConstraints; numConstraints <= maxNumConstraints; numConstraints++) {

                PreferenceDatabase db =
                        new DummyFeasiblePreferenceDatabase(-PreferenceLearning.C, PreferenceLearning.C, 0, 5, numConstraints, numDims, numberOfItems /*number of items*/);

                XADD context = new XADD();
                PreferenceLearning learning = new PreferenceLearning(context, db, indicatorNoise, "w");

                long time1start = System.currentTimeMillis();
                // Pr(W | R^{n+1})
                XADD.XADDNode nonReducedPosterior = learning.computePosteriorWeightVector(false);
                int nonReducedPosteriorNodeCount = nonReducedPosterior.collectNodes().size();
                long time2posteriorCalculated = System.currentTimeMillis();
                Integer reducedId = context.reduceLP(context._hmNode2Int.get(nonReducedPosterior));
                int reducedPosteriorNodeCount = context.getExistNode(reducedId).collectNodes().size();
                long time3posteriorReduced = System.currentTimeMillis();
                if (reducedPosteriorNodeCount - nonReducedPosteriorNodeCount > 10) {
                    context.getGraph(context._hmNode2Int.get(nonReducedPosterior)).launchViewer("nonReduced");

                    context.getGraph(reducedId).launchViewer("Reduced");

                    return;

                }

                 System.out.println(numDims + " \t\t " + numConstraints + "\t\t\t\t" +
                        (time2posteriorCalculated - time1start) + "\t\t\t\t " +
                        (time3posteriorReduced - time2posteriorCalculated) + "\t\t ||\t\t (" +
                        nonReducedPosteriorNodeCount + ")\t\t (" + reducedPosteriorNodeCount + ")");

            } // end numConstraints for
        } // end numDim for

    }


    private void fixVarLimits(XADD context, XADD.XADDNode root, double varMin, double varMax) {
        HashSet<String> vars = root.collectVars();
        for (String var : vars) {
            context._hmMinVal.put(var, varMin);
            context._hmMaxVal.put(var, varMax);
        }
    }


}
