package hgm.asve.cnsrv.approxator;

import org.junit.Test;
import xadd.XADD;
import xadd.XADDUtils;

/**
 * Created by Hadi Afshar.
 * Date: 16/01/14
 * Time: 6:23 PM
 */
public class LeafThresholdXaddApproximatorTest {
    public static void main(String[] args) {
        LeafThresholdXaddApproximatorTest instance = new LeafThresholdXaddApproximatorTest();
        instance.testBasic();
    }

    @Test
    public void testApproximateXadd() throws Exception {

    }

    @Test
    public void testBasic() {
        double minAcceptableValueToMaxLeafValueRatio = 0.2;
        XADD context = new XADD();

        String xaddString =
                "( [(1 * x) < 0]" +
                        "       ( [10] )" +
                        "       ( [((1 * x)) > 10]" +
                        "               ( [0] )" +
                        "               ( [(1*x) <= 2]" +
                        "                       ( [(10)] )" +
                        "                       ( [(1*x) <= 2.1]" +
                        "                               ([0])" +
                        "                               ([(1*x) <= 4.1] " +
                        "                                       ([100]) " +
                        "                                       ( [(1*x) <= 5] " +
                        "                                               ( [5] )" +
                        "                                               ( [1*x<-666] ( [1*x>1000] ([666]) (777)) ([1.3]) )" +      //1.3
                        "                                       )" +
                        "                               ) " +
                        "                       )" +
                        "                )" +
                        "        )" +
                        ")";

        int rootId = context.buildCanonicalXADDFromString(xaddString);
        context.getGraph(rootId).launchViewer("original");
        XADDUtils.PlotXADD(context, rootId, -2, 0.01, 15, "x", "ORIGINAL GRAPH test");

        int reducedId = context.reduceLP(rootId);
        context.getGraph(reducedId).launchViewer("reduced");
        XADDUtils.PlotXADD(context, reducedId, -2, 0.01, 15, "x", "reduced");


        LeafThresholdXaddApproximator approximator = new LeafThresholdXaddApproximator(context, minAcceptableValueToMaxLeafValueRatio);
        XADD.XADDNode approxNode = approximator.approximateXadd(context.getExistNode(
                reducedId
                //rootId
        ));


        XADDUtils.PlotXADD(context, context._hmNode2Int.get(approxNode), -2, 0.01, 15, "x", "leaf approximator");
        context.getGraph(context._hmNode2Int.get(approxNode)).launchViewer();

    }
}
