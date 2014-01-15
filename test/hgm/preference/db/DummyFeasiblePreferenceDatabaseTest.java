package hgm.preference.db;

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
        instance.testLPSolverBug();
    }
    @Test
    public void basicTest() {
        XADD context = new XADD();
        DummyFeasiblePreferenceDatabase db = new DummyFeasiblePreferenceDatabase(-PreferenceLearning.C, PreferenceLearning.C, 0, 5, 1/*constraints*/, 2, 120);
        PreferenceLearning learning = new PreferenceLearning(context, db, 0.0/*noise*/, "w");

        // Pr(W | R^{n+1})
        XADD.XADDNode posteriorUtilityWeights = learning.computePosteriorWeightVector(false);
        fixVarLimits(context, posteriorUtilityWeights, -30d, 60d);

        context.getGraph(context._hmNode2Int.get(posteriorUtilityWeights)).launchViewer("test");
        XaddVisualizer.visualize(posteriorUtilityWeights, "test", context);

        double[] initialSample = db.getAuxiliaryWeightVector();
        System.out.println("Arrays.toString(initialSample) = " + Arrays.toString(initialSample));

        VarAssignment initSampleAssign = learning.generateAWeightVectorHighlyProbablePosteriorly();
        System.out.println("initSampleAssign = " + initSampleAssign);

        Double eval = context.evaluate(context._hmNode2Int.get(posteriorUtilityWeights), initSampleAssign.getBooleanVarAssign(), initSampleAssign.getContinuousVarAssign());
        System.out.println("eval = " + eval);

        Assert.assertTrue(eval > 0.0);


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


}
