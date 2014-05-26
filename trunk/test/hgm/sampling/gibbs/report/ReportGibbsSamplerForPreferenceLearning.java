package hgm.sampling.gibbs.report;

import hgm.asve.cnsrv.approxator.LeafThresholdXaddApproximator;
import hgm.poly.bayesian.PriorHandler;
import hgm.preference.Choice;
import hgm.preference.XaddBasedPreferenceLearning;
import hgm.preference.XaddBasedPreferenceLearningTest;
import hgm.preference.Preference;
import hgm.preference.db.DummyFeasiblePreferenceDatabase;
import hgm.preference.db.PreferenceDatabase;
import hgm.sampling.XaddSampler;
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
//        instance.effectOfLPReduce();
//        instance.effectOfLeafApproximation();
        instance.leafApproximationPerformance();

    }

    PreferenceDatabase testDB1 = new PreferenceDatabase(null) {
        Preference[] prefs = new Preference[]{
                new Preference(1, 2, Choice.FIRST),
                new Preference(1, 3, Choice.FIRST),
//                new Preference(2, 3, Choice.FIRST),
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
        public int getNumberOfParameters() {
            return items.get(0).length;
        }

        @Override
        public int getNumberOfItems() {
            return items.size();
        }

        @Override
        public List<Preference> getObservedDataPoints() {
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
        XaddBasedPreferenceLearning learning = new XaddBasedPreferenceLearning(context, testDB1, 0.2, "w", 0d);
//        PreferenceLearning learning = new PreferenceLearning(context, new DummyPreferenceDatabase(-PreferenceLearning.C, PreferenceLearning.C, 0, 5, 5, 2, 120), 0.2, false);

        // Pr(W | R^{n+1})
        XADD.XADDNode utilityWeights = learning.computePosteriorWeightVector(false);
        fixVarLimits(context, utilityWeights, -30d, 60d);

        context.getGraph(context._hmNode2Int.get(utilityWeights)).launchViewer("test");
        XaddVisualizer.visualize(utilityWeights, "test", context);


        //now I sample from it:
        XaddSampler sampler = new GibbsSampler(context, utilityWeights);
        for (int i = 0; i < 50; i++) {
            VarAssignment assign = sampler.sample();
            System.out.println("t = " + assign);
        }

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

                    PreferenceDatabase db = XaddBasedPreferenceLearningTest.generateDummyPreferenceDatabase(0, 5, numConstraints, numDims, numberOfItems /*number of items*/);
                    shouldGenerateAnotherDatabase = false; //since there is no reason the current DB is not good enough

                    XADD context = new XADD();
                    XaddBasedPreferenceLearning learning = new XaddBasedPreferenceLearning(context, db, 0.1, "w", 0);

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
        int maxDim = 12;
        int minNumConstraints = 0;
        int maxNumConstraints = 10;
        double indicatorNoise = 0.1;
        boolean reduceLP = true;
        int numberOfSamplesFromEachDatabase = 10000;

        System.out.println("#Dims| #Constraints|  Time:Posterior|  #Nodes:posterior|Time:CDF| Time:sampling");
        for (int numDims = minDim; numDims <= maxDim; numDims++) {
            for (int numConstraints = minNumConstraints; numConstraints <= maxNumConstraints; numConstraints++) {

                PreferenceDatabase db =
                        new DummyFeasiblePreferenceDatabase(
//                                -PreferenceLearning.C, PreferenceLearning.C, 0, 5, numConstraints, numDims, numberOfItems /*number of items*/);
                                0, 5, numConstraints, PriorHandler.uniformInHypercube("w", numDims, XaddBasedPreferenceLearning.C), numberOfItems /*number of items*/);

                XADD context = new XADD();
                XaddBasedPreferenceLearning learning = new XaddBasedPreferenceLearning(context, db, indicatorNoise, "w", 0);

                long time1start = System.currentTimeMillis();
                // Pr(W | R^{n+1})
                XADD.XADDNode posterior = learning.computePosteriorWeightVector(reduceLP);
                int posteriorNodeCount = posterior.collectNodes().size();
                long time2posteriorCalculated = System.currentTimeMillis();


                //This should only be used for visualization (???), not in sampling ?
                fixVarLimits(context, posterior, -0.5d, 5.5d);  //todo! is it wrong? w from .<-c to C<.

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
                    if (eval == null || eval <= 0.0) System.out.println("eval = " + eval);
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
        int maxDim = 12;
        int minNumConstraints = 0;
        int maxNumConstraints = 40;
        double indicatorNoise = 0.1;
        boolean reduceLP = true;
        int numberOfSamplesFromEachDatabase = 10000;

        System.out.println("#Dims| #Constraints|  Time:Posterior |   Time:LP || #Nodes:-LP | #Nodes:+LP ");
        for (int numDims = minDim; numDims <= maxDim; numDims++) {
            for (int numConstraints = minNumConstraints; numConstraints <= maxNumConstraints; numConstraints++) {

                PreferenceDatabase db =
                        new DummyFeasiblePreferenceDatabase(
//                                -PreferenceLearning.C, PreferenceLearning.C, 0, 5, numConstraints, numDims, numberOfItems /*number of items*/);
                                0, 5, numConstraints, PriorHandler.uniformInHypercube("w", numDims, XaddBasedPreferenceLearning.C), numberOfItems /*number of items*/);

                XADD context = new XADD();
                XaddBasedPreferenceLearning learning = new XaddBasedPreferenceLearning(context, db, indicatorNoise, "w", 0);

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

    @Test
    public void effectOfLeafApproximation() {
        int numberOfItems = 200; // shouldn't have any significant effect (?) unless if its too small, dummy items will be repeated...
        int minDim = 1;
        int maxDim = 12;
        int minNumConstraints = 0;
        int maxNumConstraints = 20;
        double indicatorNoise = 0.1;
        boolean reduceLP = false; //should be false to check its effect independently
        int numberOfSamplesFromEachDatabase = 10000;

        FunctionStat u = new FunctionStat() {
            @Override
            public double value(VarAssignment valuation) {
                double summation = 0.0;
                for (Double d : valuation.getContinuousVarAssign().values()) {
                    summation += d;
                }
                return summation;
            }
        };

        System.out.println("#Dims| #Constraints|  Time:Posterior |   Time:LP || #Nodes:-LP | #Nodes:+LP ");
        for (int numDims = minDim; numDims <= maxDim; numDims++) {
            for (int numConstraints = minNumConstraints; numConstraints <= maxNumConstraints; numConstraints++) {

                PreferenceDatabase db =
                        new DummyFeasiblePreferenceDatabase(
//                                -PreferenceLearning.C, PreferenceLearning.C, 0, 5, numConstraints, numDims, numberOfItems /*number of items*/);
                                0, 5, numConstraints, PriorHandler.uniformInHypercube("w", numDims, XaddBasedPreferenceLearning.C), numberOfItems /*number of items*/);

                XADD context = new XADD();
                XaddBasedPreferenceLearning learning = new XaddBasedPreferenceLearning(context, db, indicatorNoise, "w", 0);

                long time1start = System.currentTimeMillis();

                //POSTERIOR:
                // Pr(W | R^{n+1})
                XADD.XADDNode nonReducedPosterior = learning.computePosteriorWeightVector(reduceLP);
                int nonReducedPosteriorNodeCount = nonReducedPosterior.collectNodes().size();

                long time2posteriorCalculated = System.currentTimeMillis();

                //REDUCE
                Integer reducedPosteriorId = context.reduceLP(context._hmNode2Int.get(nonReducedPosterior));
                XADD.XADDNode reducedPosterior = context._hmInt2Node.get(reducedPosteriorId);
                int reducedPosteriorNodeCount = reducedPosterior.collectNodes().size();

                long time3posteriorReduced = System.currentTimeMillis();

                //SAMPLE FROM REDUCED POSTERIOR
                fixVarLimits(context, reducedPosterior, -5.5d, 5.5d);   //todo???
                GibbsSampler sampler = new GibbsSampler(context, reducedPosterior, learning.generateAWeightVectorHighlyProbablePosteriorly());
                long time4samplerInitialized = System.currentTimeMillis();

                u.reset();
                for (int i = 0; i < numberOfSamplesFromEachDatabase; i++) {
                    VarAssignment assign = sampler.sample();
                    u.addSample(assign);

                    //make sure the taken sample is positive:
                    Double eval = context.evaluate(reducedPosteriorId, assign.getBooleanVarAssign(), assign.getContinuousVarAssign());
                    if (eval == null || eval <= 0.0) System.out.println("eval = " + eval);
                    Assert.assertTrue(eval > 0.0);
                }
                System.out.println("reduced.sum.getMean() = " + u.getMean() + " +- " + u.getStandardErrorOfTheMean());


                long time5samplesTaken = System.currentTimeMillis();

                //LEAF-APPROX POSTERIOR:
                LeafThresholdXaddApproximator approximator = new LeafThresholdXaddApproximator(context, 0.01);
                XADD.XADDNode approxReducedPosterior = approximator.approximateXadd(reducedPosterior);
                int approxReducedPosteriorNodeCount = approxReducedPosterior.collectNodes().size();
                int approxReducedPosteriorId = context._hmNode2Int.get(approxReducedPosterior);
                fixVarLimits(context, approxReducedPosterior, -5.5d, 5.5d);   //todo...
//                XaddVisualizer.visualize(reducedPosterior, "reduced", context);
//                XaddVisualizer.visualize(approxReducedPosterior, "approx", context);

                long time6posteriorApproximated = System.currentTimeMillis();

                //SAMPLING FROM APPROXIMATED REDUCED POSTERIOR:
                sampler = new GibbsSampler(context, approxReducedPosterior,
                        learning.generateAWeightVectorHighlyProbablePosteriorly());  //should work (?)
                long time7samplerOfApproxInitialized = System.currentTimeMillis();

                u.reset();
                for (int i = 0; i < numberOfSamplesFromEachDatabase; i++) {
                    VarAssignment assign = sampler.sample();
                    u.addSample(assign);

                    //make sure the taken sample is positive:
                    Double eval = context.evaluate(approxReducedPosteriorId, assign.getBooleanVarAssign(), assign.getContinuousVarAssign());
                    if (eval == null || eval <= 0.0) System.out.println("eval = " + eval);
                    Assert.assertTrue(eval > 0.0);

                }
                System.out.println("approx.sum.getMean() = " + u.getMean() + " +- " + u.getStandardErrorOfTheMean());

                long time8samplingFromApproxFinished = System.currentTimeMillis();


                System.out.println("dim:" + numDims + " \t" +
                        "#constraints: " + numConstraints + "\t" +
                        "posterior.calc.time: " + (time2posteriorCalculated - time1start) + "\t" +
                        "reduce.time: " + (time3posteriorReduced - time2posteriorCalculated) + "\t" +
                        "sampler.init.time:" + (time4samplerInitialized - time3posteriorReduced) + "\t" +
                        "sampling.time:" + (time5samplesTaken - time4samplerInitialized) + "\t" +
                        "approx.time:" + (time6posteriorApproximated - time5samplesTaken) + "\t" +
                        "approx.sampler.init.time:" + (time7samplerOfApproxInitialized - time6posteriorApproximated) + "\t" +
                        "approx.sampling.time:" + (time8samplingFromApproxFinished - time7samplerOfApproxInitialized) + "\t\t ||\t\t (" +
                        "origin #nodes:" + nonReducedPosteriorNodeCount + ")\t" +
                        "reduced#nodes:" + "(" + reducedPosteriorNodeCount + ")\t" +
                        "approx #nodes:" + "(" + approxReducedPosteriorNodeCount + ")");

            } // end numConstraints for
        } // end numDim for

    }

    @Test
    public void leafApproximationPerformance() {
        int numberOfItems = 200; // shouldn't have any significant effect (?) unless if its too small, dummy items will be repeated...
        int minDim = 2;
        int maxDim = 30;
        int minNumConstraints = 0;
        int maxNumConstraints = 30;
        double indicatorNoise = 0.1;
//        boolean reduceLP = true;
        int numberOfSamplesFromEachDatabase = 10000;
        double minAcceptableValueToMaxLeafValueRatio = 0.01;

//        System.out.println("#Dims| #Constraints|  Time:Posterior |   Time:LP || #Nodes:-LP | #Nodes:+LP ");
        for (int numDims = minDim; numDims <= maxDim; numDims++) {
            for (int numConstraints = minNumConstraints; numConstraints <= maxNumConstraints; numConstraints++) {

                PreferenceDatabase db =
                        new DummyFeasiblePreferenceDatabase(
//                                -PreferenceLearning.C, PreferenceLearning.C, 0, 5, numConstraints, numDims, numberOfItems);
                        0, 5, numConstraints, PriorHandler.uniformInHypercube("w", numDims, XaddBasedPreferenceLearning.C), numberOfItems /*number of items*/);

                XADD context = new XADD();
                XaddBasedPreferenceLearning learning = new XaddBasedPreferenceLearning(context, db, indicatorNoise, "w", 0);

                long time1start = System.currentTimeMillis();

                //POSTERIOR:
                // Pr(W | R^{n+1})
                XADD.XADDNode reducedPosterior = learning.computePosteriorWeightVector(true /*reduceLP*/, -0.1 /*no mass threshold*/); //reduce LP as well...
                int reducedPosteriorNodeCount = reducedPosterior.collectNodes().size();
                int reducedPosteriorId = context._hmNode2Int.get(reducedPosterior);

                long time2posteriorCalculated = System.currentTimeMillis();

                //REDUCE
//                Integer reducedPosteriorId = context.reduceLP(context._hmNode2Int.get(nonReducedPosterior));
//                XADD.XADDNode reducedPosterior = context._hmInt2Node.get(reducedPosteriorId);
//                int reducedPosteriorNodeCount = reducedPosterior.collectNodes().size();
//
//                long time3posteriorReduced = System.currentTimeMillis();
//


                //SAMPLE FROM REDUCED POSTERIOR
                /*fixVarLimits(context, reducedPosterior, -5.5d, 5.5d);   //todo???
                GibbsSampler sampler = new GibbsSampler(context, reducedPosterior, learning.generateAWeightVectorHighlyProbablePosteriorly());
                long time4samplerInitialized = System.currentTimeMillis();


                for (int i = 0; i < numberOfSamplesFromEachDatabase; i++) {
                    VarAssignment assign = sampler.sample();

                    //make sure the taken sample is positive:
                    Double eval = context.evaluate(reducedPosteriorId, assign.getBooleanVarAssign(), assign.getContinuousVarAssign());
                    if (eval == null || eval<=0.0) System.out.println("eval = " + eval);
                    Assert.assertTrue(eval > 0.0);
                }
*/


                long time5samplesTaken = System.currentTimeMillis();

                //LEAF-APPROX POSTERIOR:
                LeafThresholdXaddApproximator approximator = new LeafThresholdXaddApproximator(context, minAcceptableValueToMaxLeafValueRatio);
                XADD.XADDNode approxReducedPosterior = approximator.approximateXadd(reducedPosterior);
                int approxReducedPosteriorId = context._hmNode2Int.get(approxReducedPosterior);

                context.addSpecialNode(approxReducedPosteriorId);
                //I do not need the not approximated version any more....
//                context._hsSpecialNodes.remove(reducedPosteriorId);
                context.flushCaches();

                int approxReducedPosteriorNodeCount = approxReducedPosterior.collectNodes().size();
                fixVarLimits(context, approxReducedPosterior, -5.5d, 5.5d);   //todo...
//                XaddVisualizer.visualize(reducedPosterior, "reduced", context);
//                XaddVisualizer.visualize(approxReducedPosterior, "approx", context);

                long time6posteriorApproximated = System.currentTimeMillis();

                //SAMPLING FROM APPROXIMATED REDUCED POSTERIOR:
                GibbsSampler sampler = new GibbsSampler(context, approxReducedPosterior,
                        learning.generateAWeightVectorHighlyProbablePosteriorly());  //should work (?)
                long time7samplerOfApproxInitialized = System.currentTimeMillis();

//                u.reset();
                for (int i = 0; i < numberOfSamplesFromEachDatabase; i++) {
                    VarAssignment assign = sampler.sample();
//                    u.addSample(assign);

                    //make sure the taken sample is positive:
                    Double eval = context.evaluate(approxReducedPosteriorId, assign.getBooleanVarAssign(), assign.getContinuousVarAssign());
                    if (eval == null || eval <= 0.0) System.out.println("eval = " + eval);
                    Assert.assertTrue(eval > 0.0);

                }
//                System.out.println("approx.sum.getMean() = " + u.getMean() + " +- " + u.getStandardErrorOfTheMean());

                long time8samplingFromApproxFinished = System.currentTimeMillis();


                System.out.println("dim:" + numDims + " \t" +
                        "#constraints: " + numConstraints + "\t" +
                        "posterior.calc.time: " + (time2posteriorCalculated - time1start) + "\t" +
//                        "reduce.time: "+ (time3posteriorReduced - time2posteriorCalculated) + "\t" +
//                        "sampler.init.time:"+ (time4samplerInitialized -time3posteriorReduced) + "\t" +
//                        "sampling.time:"+ (time5samplesTaken - time4samplerInitialized) + "\t" +
                        "approx.time:" + (time6posteriorApproximated - time5samplesTaken) + "\t" +
                        "approx.sampler.init.time:" + (time7samplerOfApproxInitialized - time6posteriorApproximated) + "\t" +
                        "approx.sampling.time:" + (time8samplingFromApproxFinished - time7samplerOfApproxInitialized) + "\t\t ||\t\t (" +
//                        "origin #nodes:"+ nonReducedPosteriorNodeCount + ")\t" +
                        "reduced#nodes:" + "(" + reducedPosteriorNodeCount + ")\t" +
                        "approx #nodes:" + "(" + approxReducedPosteriorNodeCount + ")");

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

    abstract class FunctionStat {
        private int sampleCounter;
        private double valueSum;
        private double sqValueSum;

        protected FunctionStat() {
            reset();
        }

        public void addSample(VarAssignment assignment) {
            sampleCounter++;
            double v = value(assignment);
            valueSum += v;
            sqValueSum += v * v;
        }

        abstract double value(VarAssignment valuation);

        public double getMean() {
            return valueSum / (double) sampleCounter;
        }

        public double getVariance() {
            return sqValueSum / (double) sampleCounter;
        }

        public double getStandardErrorOfTheMean() {
            return Math.sqrt(getVariance()) / Math.sqrt(sampleCounter);
        }

        public void reset() {
            sampleCounter = 0;
            valueSum = 0d;
            sqValueSum = 0d;
        }
    }


}
