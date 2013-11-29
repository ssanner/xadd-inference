package hgm.asve.cnsrv.approxator.regression;

import hgm.XaddVisualizer;
import hgm.asve.cnsrv.approxator.Approximator;
import hgm.asve.cnsrv.approxator.regression.measures.MeanSquareErrorMeasure;
import hgm.asve.cnsrv.factor.Factor;
import hgm.asve.cnsrv.factory.ModelBasedXaddFactorFactory;
import hgm.asve.cnsrv.gm.FBQuery;
import hgm.asve.cnsrv.infer.ExactSveInferenceEngine;
import org.junit.Test;
import xadd.XADD;

/**
 * Created by Hadi Afshar.
 * Date: 25/10/13
 * Time: 11:20 AM
 */
public class RegressionBasedXaddApproximatorTest {

    public static void main(String[] args) throws Exception {
        RegressionBasedXaddApproximatorTest instance = new RegressionBasedXaddApproximatorTest();
//        instance.testLeafPowerDecrease();
        instance.testLeafPowerDecreaseOnExactSVE("./src/sve/radar.gm", "./src/sve/radar.query.4");
    }

    @Test
    public void testBasicXaddApproximateByMerge() throws Exception {
        final int MAX_POWER = 1;
        final int SAMPLE_NUM_PER_CONTINUOUS_VAR = 40;
        final double REGULARIZATION_COEFFICIENT = 0.1;
//        final double MAX_ACCEPTABLE_MEAN_SQUARED_ERROR_PER_SIBLING_MERGE = 0.1;//Double.MAX_VALUE;//1000000000d;
//        final int TRIGGER = 2;

        XADD context = new XADD();
        int rootId = context.buildCanonicalXADDFromString("" +
                "([(1 * x) < -18] " +
                "   ([0]) " +
                "   ([(1 * x) > 3]" +
                "       ([(x + y) > 10] ([2.0]) ([0])) " +
                "       ([1 * y < -3] " +
                "           ([0.01 * y*y])" +
                "           ([1 * y > 3] " +
                "               ([0.005*x*x + 0.01*y*y + 0.05*y])" +
                "               ([0.03*x + 0.03*y + 1.0])" +
                "           )" +
                "       )" +
                "   )" +
                ")");
        context._hmMinVal.put("x", -10d);
        context._hmMinVal.put("y", -10d);
        context._hmMaxVal.put("x", 10d);
        context._hmMaxVal.put("y", 10d);
        XADD.XADDNode root = context._hmInt2Node.get(rootId);
        context.getGraph(rootId).launchViewer("root");
        XaddVisualizer.visualize(root, "root", context);

        RegressionBasedXaddApproximator approx = new RegressionBasedXaddApproximator(context, new MeanSquareErrorMeasure(),
                MAX_POWER, SAMPLE_NUM_PER_CONTINUOUS_VAR, REGULARIZATION_COEFFICIENT) {
            @Override
            public XADD.XADDNode approximateXadd(XADD.XADDNode root) {
                return null;  //not used
            }
        };
        XADD.XADDNode approxRoot = approx.approximateXadd(root);

//        context.getGraph(context._hmNode2Int.get(approxRoot)).launchViewer("approx node");
        XaddVisualizer.visualize(approxRoot, "approxRoot", context);
    }

    @Test
    public void testLeafPowerDecrease() throws Exception {
        final int MAX_POWER = 2;
        final int SAMPLE_NUM_PER_CONTINUOUS_VAR = 4;
        final double REGULARIZATION_COEFFICIENT = 0.000001;
//        final int TRIGGER = 2;

        XADD context = new XADD();
        int rootId = context.buildCanonicalXADDFromString("" +
                "([(1 * x) < -5] " +
                "   ([0]) " +
                "   ([(1 * x) > 5]" +
                "       ([0]) " +
                "       ([1 * y < -5] " +
                "           ([0])" +
                "           ([1 * y > 5] " +
                "               ([0])" +
                "               ([x*x*x*x*2 + x*x + x*y*y*y + 0.69])" +
                "           )" +
                "       )" +
                "   )" +
                ")");
        context._hmMinVal.put("x", -10d);
        context._hmMinVal.put("y", -10d);
        context._hmMaxVal.put("x", 10d);
        context._hmMaxVal.put("y", 10d);
        XADD.XADDNode root = context._hmInt2Node.get(rootId);
//        context.getGraph(rootId).launchViewer("root");
        XaddVisualizer.visualize(root, "root", context);

        RegressionBasedXaddApproximator approx = new RegressionBasedXaddApproximator(context, new MeanSquareErrorMeasure(),
                MAX_POWER, SAMPLE_NUM_PER_CONTINUOUS_VAR, REGULARIZATION_COEFFICIENT) {
            @Override
            public XADD.XADDNode approximateXadd(XADD.XADDNode root) {
                return null;
            }
        };
        XADD.XADDNode approxRoot = approx.approximateXaddByLeafPowerDecreaseWithoutMerging(root);

        context.getGraph(context._hmNode2Int.get(approxRoot)).launchViewer("approx node");
        XaddVisualizer.visualize(approxRoot, "approxRoot", context);
    }

    public void testLeafPowerDecreaseOnExactSVE(String gmFile, String qFile) {
        FBQuery q = new FBQuery(qFile);
        ModelBasedXaddFactorFactory factory = ModelBasedXaddFactorFactory.newInstance(gmFile, q, Approximator.DUMMY_APPROXIMATOR /*no approximator*/);

        ExactSveInferenceEngine exact = new ExactSveInferenceEngine(factory);
        Factor exactResultF = exact.infer(true);
        factory.getVisualizer().visualizeFactor(exactResultF, "exact");

//        CurveFittingBasedXaddApproximator approx = new CurveFittingBasedXaddApproximator(factory.getContext(), new MeanSquareErrorMeasure());
        RegressionBasedXaddApproximator approx = new RegressionBasedXaddApproximator(factory.getContext(), new MeanSquareErrorMeasure(),
                2, 800/*SAMPLE_NUM_PER_CONTINUOUS_VAR*/, 0.1 /*REGULARIZATION_COEFFICIENT*/) {
            @Override
            public XADD.XADDNode approximateXadd(XADD.XADDNode root) {
                return null;
            }
        };
        XADD.XADDNode approxRoot = approx.approximateXaddByLeafPowerDecreaseWithoutMerging(factory.getContext()._hmInt2Node.get(exactResultF.getXaddId()));
        XaddVisualizer.visualize(approxRoot, "approx", factory.getContext());
    }

}
