package hgm.preference.db;

import hgm.asve.cnsrv.approxator.LeafThresholdXaddApproximator;
import hgm.preference.PreferenceLearning;
import hgm.sampling.VarAssignment;
import hgm.utils.vis.XaddVisualizer;
import org.junit.Assert;
import org.junit.Test;
import xadd.XADD;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by Hadi Afshar.
 * Date: 13/01/14
 * Time: 6:45 PM
 */
public class DummyFeasiblePreferenceDatabaseTest {
    public static void main(String[] args) {
        DummyFeasiblePreferenceDatabaseTest instance = new DummyFeasiblePreferenceDatabaseTest();
//        instance.basicTest();
//        instance.testLPSolverBug();
        instance.approximationTest();
    }
    @Test
    public void basicTest() {
        XADD context = new XADD();
        DummyFeasiblePreferenceDatabase db = new DummyFeasiblePreferenceDatabase(-PreferenceLearning.C, PreferenceLearning.C, 0, 5, 35/*constraints*/, 2, 120);
        PreferenceLearning learning = new PreferenceLearning(context, db, 0.1/*noise*/, "w");

        // Pr(W | R^{n+1})
        XADD.XADDNode posteriorUtilityWeights = learning.computePosteriorWeightVector(false, -0.2);
        fixVarLimits(context, posteriorUtilityWeights, -30d, 60d);

//        context.getGraph(context._hmNode2Int.get(posteriorUtilityWeights)).launchViewer("test");
        XaddVisualizer.visualize(posteriorUtilityWeights, "original", context);
        System.out.println("posteriorUtilityWeights.collectNodes().size() = " + posteriorUtilityWeights.collectNodes().size());

        //init sample:
        double[] initialSample = db.getAuxiliaryWeightVector();
        System.out.println("Arrays.toString(initialSample) = " + Arrays.toString(initialSample));

        VarAssignment initSampleAssign = learning.generateAWeightVectorHighlyProbablePosteriorly();
        System.out.println("initSampleAssign = " + initSampleAssign);

        Double eval = context.evaluate(context._hmNode2Int.get(posteriorUtilityWeights), initSampleAssign.getBooleanVarAssign(), initSampleAssign.getContinuousVarAssign());
        System.out.println("eval = " + eval);

        Assert.assertTrue(eval > 0.0);

        //testing reduce...
        XADD.XADDNode reducedPosterior = context.getExistNode(context.reduceLP(context._hmNode2Int.get(posteriorUtilityWeights)));
//        fixVarLimits(context, reducedPosterior, -30d, 60d);

//        context.getGraph(context._hmNode2Int.get(reducedPosterior)).launchViewer("reduce");
        XaddVisualizer.visualize(reducedPosterior, "reduce", context);
        System.out.println("reducedPosterior.size = " + reducedPosterior.collectNodes().size());

        // testing approximation as well...

        LeafThresholdXaddApproximator approximator = new LeafThresholdXaddApproximator(context, 0.01 /*threshold*/);
        XADD.XADDNode approx = approximator.approximateXadd(reducedPosterior);
//        fixVarLimits(context, approx, -30d, 60d);

//        context.getGraph(context._hmNode2Int.get(approx)).launchViewer("approx");
        XaddVisualizer.visualize(approx, "approx", context);
        System.out.println("approx.collectNodes().size() = " + approx.collectNodes().size());

        //now I sample from it:
      /*  Sampler sampler = new GibbsSampler(context, utilityWeights);
        for (int i = 0; i < 50; i++) {
            VarAssignment assign = sampler.sample();
            System.out.println("t = " + assign);
        }*/

    }

    private void fixVarLimits(XADD context, XADD.XADDNode root, double varMin, double varMax) {
        HashSet<String> vars = root.collectVars();
        for (String var : vars) {
            context._hmMinVal.put(var, varMin);
            context._hmMaxVal.put(var, varMax);
        }
    }

    //This retrieves a BUG in LP solver version 2
    @Test
    public void testLPSolverBug() {
        XADD context = new XADD();
        int rootId = context.buildCanonicalXADDFromString("( [(-1 + (-0.9 * w_0)) > 0]\n" +
                "    ( [0] ) \n" +
                "    ( [(1 + (-0.1 * w_0)) > 0]\n" +
                "       ( [(-1 + (-0.1 * w_1)) > 0]\n" +
                "          ( [0] ) \n" +
                "          ( [(1 + (-0.1 * w_1)) > 0]\n" +
                "             ( [(-1 + (172.7329 * w_0) + (2.54739 * w_1)) > 5]\n" +
                "                ( [1.0] ) \n" +
                "                ( [0] ) )  \n" +
                "             ( [0] ) )  )  \n" +
                "       ( [0] ) )  ) ");

        context.getGraph(rootId).launchViewer("original");

        fixVarLimits(context, context.getExistNode(rootId), -30d, 60d);
        XaddVisualizer.visualize(context.getExistNode(rootId), "test", context);

        int reducedId = context.reduceLP(rootId);
        context.getGraph(reducedId).launchViewer("reduced");

    }

    @Test
    public void approximationTest() {
        XADD context = new XADD();
        DummyFeasiblePreferenceDatabase db = new DummyFeasiblePreferenceDatabase(-PreferenceLearning.C, PreferenceLearning.C, 0, 5, 3/*constraints*/, 2, 120);
        PreferenceLearning learning = new PreferenceLearning(context, db, 0.1/*noise*/, "w");

        // Pr(W | R^{n+1})
        XADD.XADDNode posteriorUtilityWeights = learning.computePosteriorWeightVector(true, 0.001);
        fixVarLimits(context, posteriorUtilityWeights, -30d, 60d);

//        context.getGraph(context._hmNode2Int.get(posteriorUtilityWeights)).launchViewer("test");
        XaddVisualizer.visualize(posteriorUtilityWeights, "original", context);
        System.out.println("posteriorUtilityWeights.collectNodes().size() = " + posteriorUtilityWeights.collectNodes().size());

/*
        //init sample:
        double[] initialSample = db.getAuxiliaryWeightVector();
        System.out.println("Arrays.toString(initialSample) = " + Arrays.toString(initialSample));

        VarAssignment initSampleAssign = learning.generateAWeightVectorHighlyProbablePosteriorly();
        System.out.println("initSampleAssign = " + initSampleAssign);

        Double eval = context.evaluate(context._hmNode2Int.get(posteriorUtilityWeights), initSampleAssign.getBooleanVarAssign(), initSampleAssign.getContinuousVarAssign());
        System.out.println("eval = " + eval);

        Assert.assertTrue(eval > 0.0);
*/

        //testing reduce...
//        XADD.XADDNode reducedPosterior = context.getExistNode(context.reduceLP(context._hmNode2Int.get(posteriorUtilityWeights)));
//        fixVarLimits(context, reducedPosterior, -30d, 60d);

//        context.getGraph(context._hmNode2Int.get(reducedPosterior)).launchViewer("reduce");
//        XaddVisualizer.visualize(reducedPosterior, "reduce", context);
//        System.out.println("reducedPosterior.size = " + reducedPosterior.collectNodes().size());

        // testing approximation as well...

//        LeafThresholdXaddApproximator approximator = new LeafThresholdXaddApproximator(context, 0.01 /*threshold*/);
//        XADD.XADDNode approx = approximator.approximateXadd(reducedPosterior);
//        fixVarLimits(context, approx, -30d, 60d);

//        context.getGraph(context._hmNode2Int.get(approx)).launchViewer("approx");
//        XaddVisualizer.visualize(approx, "approx", context);
//        System.out.println("approx.collectNodes().size() = " + approx.collectNodes().size());

        //now I sample from it:
      /*  Sampler sampler = new GibbsSampler(context, utilityWeights);
        for (int i = 0; i < 50; i++) {
            VarAssignment assign = sampler.sample();
            System.out.println("t = " + assign);
        }*/

    }


}
