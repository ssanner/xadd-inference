package hgm.asve.cnsrv.approxator;

import org.junit.Assert;
import org.junit.Test;
import xadd.XADD;

import java.util.List;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 15/10/13
 * Time: 4:46 AM
 */
public class PathIntegralOnLeafFunctionCalculatorTest {
    @Test
    public void test1() {
        final XADD context = new XADD();
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

        PathIntegralOnLeafFunctionCalculator testCalc = new PathToXaddExpansionBasedIntegralCalculator();
        LeafFunction identityFunc = LeafFunction.identityFunction();
        LeafFunction oneFunc = LeafFunction.oneFunction(context);
        Map<List<XADD.XADDNode>, Double> testPathMassMap = testCalc.calculatePathValueMap(context.getExistNode(rootId), context, identityFunc);
        Map<List<XADD.XADDNode>, Double> testPathVolumeMap = testCalc.calculatePathValueMap(context.getExistNode(rootId), context, oneFunc);

        PathIntegralOnLeafFunctionCalculator calc3 = new EfficientPathIntegralCalculator();
        Map<List<XADD.XADDNode>, Double> pathMassMap3 = calc3.calculatePathValueMap(context.getExistNode(rootId), context, identityFunc);
        Map<List<XADD.XADDNode>, Double> pathVolumeMap3 = calc3.calculatePathValueMap(context.getExistNode(rootId), context, oneFunc);

        Assert.assertEquals(testPathVolumeMap, pathVolumeMap3);
        Assert.assertEquals(testPathMassMap, pathMassMap3);
    }

}
