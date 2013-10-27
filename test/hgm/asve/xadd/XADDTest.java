package hgm.asve.xadd;

import hgm.asve.cnsrv.approxator.EfficientPathIntegralCalculator;
import hgm.asve.cnsrv.approxator.MassThresholdXaddApproximator;
import hgm.asve.cnsrv.factor.Factor;
import org.junit.Test;
import xadd.XADD;
import xadd.XADDUtils;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 24/09/13
 * Time: 11:09 PM
 */
public class  XADDTest {
    XADD context = new XADD();

    public static void main(String[] args) {
        String a = "";
        String b = null;
        if (b instanceof String) System.out.println("b is string");
        System.out.println("a==b = " + a.equals(b) );
//        XADDTest instance = new XADDTest();
//        instance.testMarginalization1();
//        instance.testXaddMultiplication();
//        instance.testBasic();
//        instance.testMultiplicationOrder();
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

   /*     String xaddString =
                "( [(-1 * x) > 0]" +   //1 * x) < 0
                        "       ( [(1*x < -5)] " +
                        "               ([0]) " +
                        "               ([1]) " +
                        "       )" +
                        "       ( [((1 * x)) > 10]" +
                        "               ( [0] )" +
                        "               ( [(1*x) <= 2]" +
                        "                       ( [2] )" +
                        "                       ( [(1*x) <= 2.1]" +
                        "                               ([10])" +
                        "                               ([(1*x) <= 4.1] " +
                        "                                       ([3])" +
                        "                                       ( [(1*x) <= 5] " +
                        "                                               ( [0] )" +
                        "                                               ([6])"+
                        "                                       )" +
                        "                               ) " +
                        "                       )" +
                        "                )" +
                        "        )" +
                        ")";
*/
        int rootId = context.buildCanonicalXADDFromString(xaddString);
//        context.getGraph(rootId).launchViewer();
        XADDUtils.PlotXADD(context, rootId, -2, 0.01, 15, "x", "ORIGINAL GRAPH test");

        MassThresholdXaddApproximator approximator = new MassThresholdXaddApproximator(context, rootId, new EfficientPathIntegralCalculator(context));
        int newRoot = approximator.approximateXADD(3, 100);//context.approximateXADD(rootId, 3, 100);
        System.out.println("context.getExistNode(newRoot) = " + context.getExistNode(newRoot));

//        boolean merged = context.mergeNodes(rootId, 1000.5, 1000);
//        System.out.println("merged = " + merged);

        XADDUtils.PlotXADD(context, newRoot, -2, 0.01, 15, "x", "APPROX test");
        context.getGraph(newRoot).launchViewer();

    }

    @Test
    public void testMultiplicationOrder() {
        String[] factorStrings = {
                "( [(1 + (-0.133333 * d)) > 0]\n" +
                        "    ( [(-1 + (0.4 * d)) > 0]\n" +
                        "       ( [(-0.76 + (-0.0408 * d * d) + (0.408 * d))] ) \n" +
                        "       ( [0.005] ) )  \n" +
                        "    ( [0.005] ) )\n",

                "( [(1 + (-0.153846 * d)) > 0]\n" +
                        "    ( [(-1 + (0.666667 * d)) > 0]\n" +
                        "       ( [(-0.3928 + (0.3264 * d) + (-0.0408 * d * d))] ) \n" +
                        "       ( [0.005] ) )  \n" +
                        "    ( [0.005] ) )\n",

                "( [(1 * d) > 0]\n" +
                        "    ( [(1 + (-0.1 * d)) > 0]\n" +
                        "       ( [0.1] ) \n" +
                        "       ( [0] ) )  \n" +
                        "    ( [0] ) )\n",

                "( [(1 + (-0.181818 * d)) > 0]\n" +
                        "    ( [0.005] ) \n" +
                        "    ( [(1 + (-0.095238 * d)) > 0]\n" +
                        "       ( [(-2.3512 + (-0.0408 * d * d) + (0.6528 * d))] ) \n" +
                        "       ( [0.005] ) )  )\n",

                "( [(1 + (-0.181818 * d)) > 0]\n" +
                        "    ( [(-1 + (2 * d)) > 0]\n" +
                        "       ( [(-0.1072 + (0.2448 * d) + (-0.0408 * d * d))] ) \n" +
                        "       ( [0.005] ) )  \n" +
                        "    ( [0.005] ) ) ]"
        };

        String[] labelStrings = {"zero", "one", "two", "three", "four"};

        int[] xaddIds = new int[factorStrings.length];

        for (int i = 0; i < factorStrings.length; i++) {
            String factorStr = factorStrings[i];
            xaddIds[i] = context.buildCanonicalXADDFromString(factorStrings[i]);
//            context.getGraph(xaddIds[i]).launchViewer();
//            XADDUtils.PlotXADD(context, xaddIds[i], -2, 0.01, 15, "d", labelStrings[i]);
        }

        ///////////////////////////////////////////////////

        int multId1 = context.ONE;
        multId1 = context.applyInt(multId1, xaddIds[0], XADD.PROD);
        multId1 = context.applyInt(multId1, xaddIds[2], XADD.PROD);
        multId1 = context.applyInt(multId1, xaddIds[4], XADD.PROD);
        multId1 = context.applyInt(multId1, xaddIds[1], XADD.PROD);
        multId1 = context.applyInt(multId1, xaddIds[3], XADD.PROD);

        XADDUtils.PlotXADD(context, multId1, -2, 0.01, 15, "d", "mult one");

        ////////////////////////////////////////////

        int multId2 = context.ONE;
        multId2 = context.applyInt(multId2, xaddIds[0], XADD.PROD);
        multId2 = context.applyInt(multId2, xaddIds[1], XADD.PROD);
        multId2 = context.applyInt(multId2, xaddIds[2], XADD.PROD);
        multId2 = context.applyInt(multId2, xaddIds[3], XADD.PROD);
        multId2 = context.applyInt(multId2, xaddIds[4], XADD.PROD);

//        context.getGraph(multId2).launchViewer();
        XADDUtils.PlotXADD(context, multId2, -2, 0.01, 15, "d", "mult two");
    }

    @Test
    public void testXaddMultiplication(){
        int id1 = context.buildCanonicalXADDFromString("" +
                "([1*x < 0] " +
                "   ([-1 * x])" +
                "   ([1 * x])" +
                ")");
        int id2 = context.applyInt(id1, id1, XADD.PROD);
        context.getGraph(id2).launchViewer();
    }

    @Test
    public void testMarginalization1(){
        int id1 = context.buildCanonicalXADDFromFile("./test/hgm/asve/xadd/xaddTest1.txt");
        context._hmMinVal.put("o_1", -10d);
        context._hmMinVal.put("x_2", -10d);
        context._hmMaxVal.put("o_1", 20d);
        context._hmMaxVal.put("x_2", 20d);

        XADD.XADDNode node1 = context.getExistNode(id1);
        Factor f = new Factor(id1,context, "?");
        for (String var : node1.collectVars()) {
            System.out.println("marginalizing var = " +var);
            f = marginalize(f, var); //visualizer.visualizeFactor(mseFactor, "after marginalizing" + var);
        }

        System.out.println("valueOfOneConstantFactor(f); = " + valueOfOneConstantFactor(f));
    }


    public Factor multiply(Collection<Factor> factors) {
        int mult_xadd = context.ONE;
        String text = "(";
        for (Factor f : factors) {
            mult_xadd = context.applyInt(mult_xadd, f._xadd, XADD.PROD);
            text += (f.getHelpingText() + ".");
        }
        return new Factor(mult_xadd, context, text.substring(0, text.length() - 1) + ")");
    }

    public Factor subtract(Factor f1, Factor f2) {
        int subXaddId = context.applyInt(f1._xadd, f2._xadd, XADD.MINUS);
        return new Factor(subXaddId, context, "MINUS(" + f1.getHelpingText() + ", " + f2.getHelpingText() + ")");
    }

    public Factor power(Factor factor, int pow) {
        List<Factor> fs = new ArrayList<Factor>(pow);
        for (int i = 0; i < pow; i++) {
            fs.add(factor);
        }
        return multiply(fs);
    }

    public double meanSquaredError(Factor f1, Factor f2) {
        Factor diff = subtract(f1, f2);  //visualizer.visualizeFactor(diff, "diff");
        Factor dif2 = power(diff, 2); //visualizer.visualizeFactor(dif2,"dif2");
        Set<String> scopeVars = dif2.getScopeVars();
//        if (scopeVars.size() != 1) throw new RuntimeException("one-variable factor expected");
//        String v = scopeVars.iterator().next();

        Factor mseFactor = dif2;
        for (String var : scopeVars) {
            System.out.println("marginalizing var = " +var);
            mseFactor = marginalize(mseFactor, var); //visualizer.visualizeFactor(mseFactor, "after marginalizing" + var);
        }
        return valueOfOneConstantFactor(mseFactor);
    }

    public Factor marginalize(Factor factor, String variable) {
        // Take appropriate action based on whether var is boolean or continuous
        int bool_var_index = context.getBoolVarIndex(variable);
        int xadd_marginal = -1;
        if (bool_var_index > 0) {
            // Sum out boolean variable
            int restrict_high = context.opOut(factor._xadd, bool_var_index, XADD.RESTRICT_HIGH);
            int restrict_low = context.opOut(factor._xadd, bool_var_index, XADD.RESTRICT_LOW);
            xadd_marginal = context.apply(restrict_high, restrict_low, XADD.SUM);
        } else {
            // Integrate out continuous variable
            xadd_marginal = context.computeDefiniteIntegral(factor._xadd, variable);
        }
        return new Factor(xadd_marginal, context, "{" + factor.getHelpingText() + "|!" + variable + "}");
    }

    public Double getMinValue(String varName) {
        return context._hmMinVal.get(varName);
    }

    public Double getMaxValue(String varName) {
        return context._hmMaxVal.get(varName);

    }

    public double valueOfOneConstantFactor(Factor constFactor) {
        return context.evaluate(constFactor._xadd, new HashMap<String, Boolean>() /*EMPTY_BOOL*/, new HashMap<String, Double>()/*EMPTY_DOUBLE*/);
    }


}
