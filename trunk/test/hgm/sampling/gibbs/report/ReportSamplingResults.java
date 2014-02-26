package hgm.sampling.gibbs.report;

import hgm.sampling.XaddSampler;
import hgm.sampling.VarAssignment;
import hgm.sampling.gibbs.GibbsSampler;
import hgm.utils.vis.XaddVisualizer;
import org.junit.Test;
import xadd.XADD;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashSet;

/**
 * Created by Hadi Afshar.
 * Date: 8/01/14
 * Time: 4:05 AM
 */
public class ReportSamplingResults {
    public static void main(String[] args) throws Exception {
        ReportSamplingResults instance = new ReportSamplingResults();
//        instance.testTriangularUniform();
//        instance.testU_Quadratic();
        instance.testUniformInTriangle();
    }

    public static final String SCATTER_2D_FILENAME = "./test/hgm/sampling/gibbs/report/scatter2D.txt";

    @Test
    public void testUniformInTriangle() throws FileNotFoundException {
        //  c   /\
        //     / |\
        //  0 /  | \
        //   a   0  b
        XADD context = new XADD();
        double a = -30.0;
        double b = 10.0;
        double w = -b/a;
        int numSamples = 1000;

        int nodeId = context.buildCanonicalXADDFromString(
                ("([1*x > 0] " +
                        "([1*x + 1*y < B] " +
                        "       ([1*x - W*y < B] ([1]) ([0])) " +
                        "       ([0]) ) " +
                        "([0]))").replaceAll("B", "" + b).replaceAll("W", ""+ w));

//        int uniformId = context.buildCanonicalXADDFromString("([U(y, " + (u1 + u2) / 2.0 + ", " + (u2 - u1) / 2.0 + ", " + (u2 - u1) / 2.0 + ")])");
        XADD.XADDNode func = context._hmInt2Node.get(nodeId);
        fixVarLimits(context, func, -40d, 20d);
        XaddVisualizer.visualize(func, "func", context);

        GibbsSampler sampler = new GibbsSampler(context, func);
        sample2DToMatlabFile("x", "y", numSamples, sampler);
    }

    @Test
    public void testU_Quadratic() throws FileNotFoundException {
        XADD context = new XADD();
        double a = 10;
        double c = 1/(5*a);
        int numSamples = 10000;

        int nodeId = context.buildCanonicalXADDFromString(
                "([(1*x + 1*y) < O] ([(1*x + -1*y < O)] ([(-1*x + 1*y < O)] ([-1*x + -1*y < O] ([C*x*x + C*y*y]) ([0]))([0])) ([0]))([0]))".replaceAll("O", "" + a).replaceAll("C", ""+ c));

        XADD.XADDNode func = context._hmInt2Node.get(nodeId);
        fixVarLimits(context, func, -20d, 20d);
        XaddVisualizer.visualize(func, "func", context);

        GibbsSampler sampler = new GibbsSampler(context, func);
        sample2DToMatlabFile("x", "y", numSamples, sampler);
    }

    @Test
    public void testTriangularUniform() throws FileNotFoundException {
        /*
          /\
         / |\
        /  | \
        a  c  b
        */
        XADD context = new XADD();
        double a = 0;
        double c = 10;
        double b = 25;
        int numSamples = 10000;
        double u1 = 0;
        double u2 = 10;
//        double epsilon = 0.2; //tolerable divergence


        int triangularId = context.buildCanonicalXADDFromString("([T(x, " + c + ", " + (c - a) + ", " + (b - c) + ")])");
        int uniformId = context.buildCanonicalXADDFromString("([U(y, " + (u1 + u2) / 2.0 + ", " + (u2 - u1) / 2.0 + ", " + (u2 - u1) / 2.0 + ")])");
        XADD.XADDNode func = context._hmInt2Node.get(context.apply(triangularId, uniformId, XADD.PROD));
        fixVarLimits(context, func, -10d, 30d);
        XaddVisualizer.visualize(func, "func", context);
        GibbsSampler sampler = new GibbsSampler(context, func);

        sample2DToMatlabFile("x", "y", numSamples, sampler);
    }

    private void sample2DToMatlabFile(String var1, String var2, int numSamples, XaddSampler sampler) throws FileNotFoundException {
        PrintStream ps;
        ps = new PrintStream(new FileOutputStream(SCATTER_2D_FILENAME));

        for (int i = 0; i < numSamples; i++) {
            VarAssignment assign = sampler.sample();
            double x1 = assign.getContinuousVar(var1);
            double x2 = assign.getContinuousVar(var2);
            ps.println(x1 + "\t" + x2);
        }

        ps.close();

    }

    private void fixVarLimits(XADD context, XADD.XADDNode root, double varMin, double varMax) {
        HashSet<String> vars = root.collectVars();
        for (String var : vars) {
            context._hmMinVal.put(var, varMin);
            context._hmMaxVal.put(var, varMax);
        }
    }


    /***************************************************************
     * T E S T   F O R   P R E F E E R E N C E - L E A R N I N G
     **************************************************************/
    //todo


}
