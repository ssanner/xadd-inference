package hgm.sampling.gibbs.integral;

import hgm.utils.vis.XaddVisualizer;
import org.junit.Test;
import xadd.ExprLib;
import xadd.XADD;

import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 20/01/14
 * Time: 1:43 PM
 */
public class OneDimIntegralTest {
    public static void main(String[] args) {
        OneDimIntegralTest instance = new OneDimIntegralTest();
        instance.basicTest2();
    }

    @Test
    public void basicTest() {

        String xaddStr = "([x<5] " +
                "   ([2*x + 1< 5] " +
                "       ([2*x > x] ([5])" +
                "           ([-5 < x] ([-1*x + 0.3]) ([0]))) " +
                "       ([4])" +
                "   ) " +
                "   ([1])" +
                ")";
        XADD context = new XADD();

        int rootId = context.buildCanonicalXADDFromString(xaddStr);
        context.getGraph(rootId).launchViewer();
        XADD.XADDNode root = context.getExistNode(rootId);
        XaddVisualizer.visualize(root, -20, 20, 0.1, "root", context);

        OneDimIntegral integral = new OneDimIntegral(context);
        System.out.println("integral.integrate() = " + integral.integrate(root));
//        context.getGraph(context._hmNode2Int.get(integralNode)).launchViewer();
//        XaddVisualizer.visualize(integralNode, -10, 10, 0.1, "integral", context);

    }

    @Test
    public void basicTest2() {

        String xaddStr = "" +
                "([x<2] " +
                "   ([0]) " +
                "   ([x<3] " +
                "      ([1])" +
                "      ([x<104] " +
                "           ([0]) " +
                "           ([x<=105] " +
                "               ([1]) " +
                "               ([x<106] ([1]) ([0]))" +
                "           )" +
                "       )" +
                "   ) " +
                ")";

        XADD context = new XADD();

        int rootId = context.buildCanonicalXADDFromString(xaddStr);
        context.getGraph(rootId).launchViewer();
        XADD.XADDNode root = context.getExistNode(rootId);
        XaddVisualizer.visualize(root, -20, 200, 0.1, "root", context);

        OneDimIntegral integral = new OneDimIntegral(context);
        Piecewise1DPolynomial integrate = integral.integrate(root);
        System.out.println("integrate = " + integrate);
    }

    //todo: feasibility is not a valid assumption since XADD reduceLP is nasty... Test for not feasible...
    //todo: even evaluation as a new XADD is nasty...

}
