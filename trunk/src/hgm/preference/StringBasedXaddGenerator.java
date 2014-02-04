package hgm.preference;

import xadd.XADD;

import java.text.DecimalFormat;
import java.util.HashSet;

/**
 * Created by Hadi Afshar.
 * Date: 4/02/14
 * Time: 8:13 AM
 */
public class StringBasedXaddGenerator {
    public DecimalFormat decimalFormat = new DecimalFormat("###.#################"); //used to convert doubles to strings //todo discuss the effect of this approximation with Scott...
    protected XADD context;

    public StringBasedXaddGenerator(XADD context) {
        this.context = context;
    }

    // I do not want to use toString() method since it may generate strings like: 5.788825006847187E-4 which cannot be parsed afterwards.
    protected String doubleToString(Double d) {
        return decimalFormat.format(d);
    }

    protected XADD.XADDNode indicator(String condition, double indicatorNoise) { //noisy indicator
//        String s = "([" + condition + "] ([" + (1-indicatorNoise) + "]) ([" + (0 + indicatorNoise) + "]))";
        String s = "([" + condition + "] ([1]) ([" + (0 + indicatorNoise) + "]))";
        int nodeId = context.buildCanonicalXADDFromString(s);
        return context.getExistNode(nodeId);
    }

    protected XADD.XADDNode multiply(XADD.XADDNode... nodes) {
        int mult_xadd = context.ONE;
        for (XADD.XADDNode n : nodes) {
            mult_xadd = context.applyInt(mult_xadd, context._hmNode2Int.get(n), XADD.PROD);
        }

        return context.getExistNode(mult_xadd);
    }

    protected XADD.XADDNode sum(XADD.XADDNode... nodes) {
        int sumXadd = context.ZERO;
        for (XADD.XADDNode n : nodes) {
            sumXadd = context.applyInt(sumXadd, context._hmNode2Int.get(n), XADD.SUM);
        }

        return context.getExistNode(sumXadd);
    }

    protected XADD.XADDNode uniformDistribution(String var, double min, double max) { //todo what if I need an non-normalized uniform...
        double u = 1.0 / (max - min);
        String s = "([" + var + "<" + min + "] ([0]) ([" + var + "<" + max + "] ([" + u + "]) ([0])))";
        int nodeId = context.buildCanonicalXADDFromString(s);
        return context.getExistNode(nodeId);
    }

    public static void fixVarLimits(XADD context, XADD.XADDNode root, double varMin, double varMax) {
        HashSet<String> vars = root.collectVars();
        for (String var : vars) {
            context._hmMinVal.put(var, varMin);
            context._hmMaxVal.put(var, varMax);
        }
    }
}
