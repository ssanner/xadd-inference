package hgm.sampling.gibbs.integral;

import hgm.utils.vis.XaddVisualizer;
import junit.framework.Assert;
import org.junit.Test;
import xadd.XADD;

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
        System.out.println("integral.integrate() = " + integral.integrate(root, "x"));
//        context.getGraph(context._hmNode2Int.get(integralNode)).launchViewer();
//        XaddVisualizer.visualize(integralNode, -10, 10, 0.1, "integral", context);

    }

    @Test
    public void basicTest2() {

        String xaddStr1 = "" +
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

        testTwoIntegratorsOn(xaddStr1);

        //test infeasibility:
        testTwoIntegratorsOn("([x<2] " +
                "   ([0]) " +
                "   ([x<3] " +
                "      ([1])" +
                "      ([x>104] " +
                "           ([0]) " +
                "           ([x<=105] " +
                "               ([1]) " +
                "               ([x<106] ([1]) ([0]))" +
                "           )" +
                "       )" +
                "   ) " +
                ")");

        //test total infeasibility:
        XADD context = new XADD();
        OneDimIntegral integral = new OneDimIntegral(context);
        Piecewise1DPolynomialUsingArithExpr zeroFunc = integral.integrate(context.getExistNode(context.buildCanonicalXADDFromString("([0])")), "x");
        Assert.assertEquals(zeroFunc.value(0d), 0d);
        Assert.assertEquals(zeroFunc.value(100d), 0d);
        Assert.assertEquals(zeroFunc.value(-100d), 0d);

        //test 0 region:
        //test infeasibility:
        Piecewise1DPolynomialUsingArithExpr infeasible = integral.integrate(context.getExistNode(context.buildCanonicalXADDFromString(
                "([x<2] " +
                "   ([0]) " +
                "   ([x>2] " +
                "      ([0])" +
                "      ([1000])" +
                "   )" +
                ")")), "x");
        Assert.assertEquals(zeroFunc.value(2d), 0d);
        Assert.assertEquals(zeroFunc.value(20d), 0d);

    }

    private void testTwoIntegratorsOn(String xaddStr) {
        XADD context = new XADD();

        int rootId = context.buildCanonicalXADDFromString(xaddStr);
//        context.getGraph(rootId).launchViewer();
        XADD.XADDNode root = context.getExistNode(rootId);
//        XaddVisualizer.visualize(root, -20, 200, 0.1, "root", context);

        OneDimIntegral integral = new OneDimIntegral(context);
        Piecewise1DPolynomialUsingArithExpr integrate = integral.integrate(root, "x");
//        System.out.println("integrate = " + integrate);

        XADD testerContext = new XADD();
        XADD.XADDNode testerRoot = testerContext.getExistNode(testerContext.reduceLP(testerContext.buildCanonicalXADDFromString(xaddStr)));
        OneDimIntegralOnInstantiatedPrunedNode testerIntegral = new OneDimIntegralOnInstantiatedPrunedNode(testerContext);
        Piecewise1DPolynomialUsingArithExpr testerIntegrate = testerIntegral.integrate(testerRoot);
        Assert.assertEquals(testerIntegrate.toString(), integrate.toString());
    }

}
