package hgm.asve.cnsrv.approxator.fitting;

import hgm.DebugIntegral.IntegralTest;
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
public class CurveFittingBasedXaddApproximatorTest {

    public static void main(String[] args) throws Exception {
        CurveFittingBasedXaddApproximatorTest instance = new CurveFittingBasedXaddApproximatorTest();
        instance.testBasic();
//        instance.testFittingOnExactSVE("./src/sve/radar.gm", "./src/sve/radar.query.4");
    }

    @Test
    public void testBasic() throws Exception {
        final int MAX_POWER = 2;
        final int SAMPLE_NUM_PER_CONTINUOUS_VAR = 4;
        final double REGULARIZATION_COEFFICIENT = 0.000001;

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
        IntegralTest.visualize(root, "root", context);

        CurveFittingBasedXaddApproximator approx = new CurveFittingBasedXaddApproximator(context);
        XADD.XADDNode approxRoot = approx.approximateXaddByLeafPowerDecrease(root, MAX_POWER, SAMPLE_NUM_PER_CONTINUOUS_VAR, REGULARIZATION_COEFFICIENT);

        context.getGraph(context._hmNode2Int.get(approxRoot)).launchViewer("approx node");
        IntegralTest.visualize(approxRoot, "approxRoot", context);
    }

    public void testFittingOnExactSVE(String gmFile, String qFile) {
        FBQuery q = new FBQuery(qFile);
        ModelBasedXaddFactorFactory factory = new ModelBasedXaddFactorFactory(gmFile, q);

        ExactSveInferenceEngine exact = new ExactSveInferenceEngine(factory);
        Factor exactResultF = exact.infer(true);
        factory.getVisualizer().visualizeFactor(exactResultF, "exact");

        CurveFittingBasedXaddApproximator approx = new CurveFittingBasedXaddApproximator(factory.getContext());
        XADD.XADDNode approxRoot = approx.approximateXaddByLeafPowerDecrease(factory.getContext()._hmInt2Node.get(exactResultF._xadd), 2, 800, 0.1);
        IntegralTest.visualize(approxRoot, "approx", factory.getContext());



//            Records exactRecords = exact.getRecords();
//            System.out.println("exactRecords = " + exactRecords);
    }

}
