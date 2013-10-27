package hgm.asve.cnsrv.approxator;

import org.junit.Test;
import xadd.XADD;
import xadd.XADDUtils;

/**
 * Created by Hadi Afshar.
 * Date: 9/10/13
 * Time: 1:29 PM
 */
public class XaddApproximatorTest {
    public static final double MASS_THRESHOLD = 1.5d;
    public static final double VOLUME_THRESHOLD = Double.POSITIVE_INFINITY;
    public static final double SIBLING_DIFF_THRESHOLD = 10000.55d;
    public static final int MAX_DESIRED_NUMBER_OF_NODES = 1;
    XADD context = new XADD();

    public static void main(String[] args) {
        XaddApproximatorTest instance = new XaddApproximatorTest();
        instance.testBasic();
    }

    @Test
    public void testBasic() {

        String xaddString =
                "( [(1 * x) < 0]" +
                        "       ( [0] )" +
                        "       ( [((1 * x)) > 10]" +
                        "               ( [0] )" +
                        "               ( [(1*x) <= 2]" +
                        "                       ( [(1 * x)] )" +  //(1*x)
                        "                       ( [(1*x) <= 2.1]" +
                        "                               ([5])" + //5
                        "                               ([(1*x) <= 4.1] " +
                        "                                       ([-1.05 + (0.5 * x)]) " +
                        "                                       ( [(1*x) <= 5] " +
                        "                                               ( [0] )" +
                        "                                               ( [1*x<-666] ( [1*x>1000] ([666]) (777)) ([1.3]) )" +      //1.3
                        "                                       )" +
                        "                               ) " +
                        "                       )" +
                        "                )" +
                        "        )" +
                        ")";

        int rootId = context.buildCanonicalXADDFromString(xaddString);
        context.getGraph(rootId).launchViewer();
        XADDUtils.PlotXADD(context, rootId, -2, 0.01, 15, "x", "ORIGINAL GRAPH test");

//        MassThresholdXaddApproximator approximator1 = new MassThresholdXaddApproximator(context, rootId, new PathToXaddExpansionBasedIntegralCalculator(context));
//        int newRoot1 = approximator1.approximateXADD(MASS_THRESHOLD, VOLUME_THRESHOLD);

        SiblingXaddApproximator approximator2 = new SiblingXaddApproximator(context,
                new EfficientPathIntegralCalculator(context));
//                new PathToXaddExpansionBasedIntegralCalculator(context));
        XADD.XADDNode newRoot2Node = approximator2.approximateXADD(context.getExistNode(rootId), MAX_DESIRED_NUMBER_OF_NODES, SIBLING_DIFF_THRESHOLD);
        int newRoot2 = context._hmNode2Int.get(newRoot2Node);

//        XADDUtils.PlotXADD(context, newRoot1, -2, 0.01, 15, "x", "mass threshold approximator");
        XADDUtils.PlotXADD(context, newRoot2, -2, 0.01, 15, "x", "sibling approximator");
        context.getGraph(newRoot2).launchViewer();

    }

}
