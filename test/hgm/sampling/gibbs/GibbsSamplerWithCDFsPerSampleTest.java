package hgm.sampling.gibbs;

import hgm.sampling.Sampler;
import hgm.sampling.VarAssignment;
import hgm.sampling.gibbs.integral.Piecewise1DPolynomial;
import hgm.utils.vis.XaddVisualizer;
import junit.framework.Assert;
import org.junit.Test;
import xadd.XADD;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Hadi Afshar.
 * Date: 19/01/14
 * Time: 9:01 PM
 */
public class GibbsSamplerWithCDFsPerSampleTest {
    public static void main(String[] args) {
        GibbsSamplerWithCDFsPerSampleTest instance = new GibbsSamplerWithCDFsPerSampleTest();
        instance.testUniform();
//        instance.makeCDFTest();
        instance.basicTest1D();
    }

    @Test
    public void testUniform() {
        XADD context = new XADD();
        double min = 0;
        double max = 8;
        int numSamples = 1000;
        double epsilon = 0.2; //tolerable divergence

        int uId = context.buildCanonicalXADDFromString("([U(x, " + (max + min) / 2.0 + ", " + (max - min) / 2.0 + ", " + (max - min) / 2.0 + ")])");
        XADD.XADDNode uniform = context._hmInt2Node.get(uId);
        fixVarLimits(context, uniform, -50d, 50d);

        Sampler sampler = new GibbsSamplerWithCDFsPerSample(context, uniform);

        testStatistics("x", numSamples, sampler,
                min, max, (min + max) / 2.0, Math.pow(max - min, 2) / 12.0, epsilon);

        System.out.println("context._alOrder.size() = " + context._alOrder.size());
    }

    @Test
    public void testTriangular() {
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
        double epsilon = 0.2; //tolerable divergence


        int triangularId = context.buildCanonicalXADDFromString("([T(x, " + c + ", " + (c-a) + ", " + (b-c) + ")])");
        XADD.XADDNode triangular = context._hmInt2Node.get(triangularId);
        fixVarLimits(context, triangular, -100d, 100d);
        Sampler sampler = new GibbsSamplerWithCDFsPerSample(context, triangular);

        testStatistics("x", numSamples, sampler,
                a, b, (a + b + c) / 3.0, (a*a + b*b + c*c -a*b -a*c -b*c)/18.0, epsilon);

        System.out.println("context._alOrder.size() = " + context._alOrder.size());
    }

    @Test
    public void testUniformInTriangle() {
        //  c   /\
        //     / |\
        //  0 /  | \
        //   a   0  b
        XADD context = new XADD();
        double a = -30.0;
        double b = 10.0;  //note: c=0
        double w = -b/a;
        int numSamples = 10000;
        double epsilon = 0.9;

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

        Sampler sampler = new GibbsSamplerWithCDFsPerSample(context, func);
        // the projection of the distribution on the Y axis is a triangular distribution:
        double c=0;
        testStatistics("y", numSamples, sampler,
                a, b, (a + b + c) / 3.0, (a*a + b*b + c*c -a*b -a*c -b*c)/18.0, epsilon);
    }

    private void testStatistics(String varName, int numSamples, Sampler sampler,
                                double min, double max, double mean, double variance, double epsilon) {
        //actual...
        double aMin = Double.POSITIVE_INFINITY;
        double aMax = Double.NEGATIVE_INFINITY;
        double aMean = 0.0;
        double aVariance = 0;

        for (int i = 0; i < numSamples; i++) {
            VarAssignment assign = sampler.sample();
//            System.out.println("sample = " + assign);
            double x = assign.getContinuousVar(varName);
            aMean += x;
            aMin = Math.min(aMin, x);
            aMax = Math.max(aMax, x);
            aVariance += Math.pow(x - mean, 2);  //well, the "real" mean is not calculated yet so it is approximate...
        }
        aMean /= (double) numSamples;
        aVariance /= (double) numSamples;

        System.out.println("For #Samples = " + numSamples);
        System.out.println("actual Min = " + aMin + "\t *** \t expected Min = " + min);
        System.out.println("actual Max = " + aMax + "\t *** \t expected Max = " + max);
        System.out.println("actual Mean = " + aMean + "\t *** \t expected mean = " + mean);
        System.out.println("actual variance = " + aVariance + "\t *** \t expected variance = " + variance);
        System.out.println("actualMin-expectedMin = " + Math.abs(aMin - min));
        System.out.println("actualMax-expectedMax = " + Math.abs(aMax - max));
        System.out.println("actualMean-expectedMean = " + Math.abs(aMean - mean));
        System.out.println("actualStandardDev-expectedStandardDev = " + Math.abs(Math.sqrt(aVariance) - Math.sqrt(variance)));
        Assert.assertTrue(Math.abs(aMin - min) < epsilon);
        Assert.assertTrue(Math.abs(aMax - max) < epsilon);
        Assert.assertTrue(Math.abs(aMean - mean) < epsilon);
        Assert.assertTrue(Math.abs(Math.sqrt(aVariance) - Math.sqrt(variance)) < epsilon);
        //        System.out.println("actualMean = " + actualMean);
//        return (actualMean - expectedMean) / (Math.sqrt(expectedVariance) / Math.sqrt(numSamples));

    }


    @Test
    public void makeCDFTest() {
        XADD context = new XADD();
        int rootId = context.buildCanonicalXADDFromString("([U(x, 10, 5,5)])");
        XADD.XADDNode root = context._hmInt2Node.get(rootId);
        fixVarLimits(context, root, -10d, 20d);

        context.getGraph(rootId).launchViewer("func");
        XaddVisualizer.visualize(root, "func", context);

        GibbsSamplerWithCDFsPerSample gs = new GibbsSamplerWithCDFsPerSample(context, root);

        Piecewise1DPolynomial cdf = gs.makeCumulativeDistributionFunction(root, "x", new VarAssignment(new HashMap<String, Boolean>(), new HashMap<String, Double>()));
        System.out.println("cdf = " + cdf);

        //todo... visualize...
//        XADD.XADDNode cdf = gs.makeCumulativeDistributionFunction(root, "x", new VarAssignment(new HashMap<String, Boolean>(), new HashMap<String, Double>()));
//        XaddVisualizer.visualize(cdf, "cdf", context);

    }

    @Test
    public void basicTest1D() {
        XADD context = new XADD();
        double originMean = 0;
        double originVariance = 5;
        int numSamples = 1000;

        int rootId = context.buildCanonicalXADDFromString("([N(x, " + originMean + ", 666, " + originVariance + ")])");
        XADD.XADDNode root = context._hmInt2Node.get(rootId);
        fixVarLimits(context, root, -30d, 30d);

//        context.getGraph(rootId).launchViewer("test");
//        XaddVisualizer.visualize(root, "test", context);

        Sampler sampler = new GibbsSamplerWithCDFsPerSample(context, root);
        double t = tTest1D("x", sampler, numSamples, originMean, originVariance);
        System.out.println("t = " + t);

        Assert.assertTrue(Math.abs(t) < 2.0);

//        double average = 0.0;
//        for (int i = 0; i < numSamples; i++) {
//            VarAssignment assign= sampler.sample();
//            System.out.println("t = " + assign);
//            average += assign.getContinuousVar("x");
//        }
//        average/=(double)numSamples;
//        System.out.println("av = " + average);
//        Assert.assertTrue(Math.abs(average - 10.0) < 0.3);
    }

    private double tTest1D(String varName, Sampler sampler, int numSamples,
                           double expectedMean, double expectedVariance) {
        double sum = 0.0;
//        double min = Double.POSITIVE_INFINITY;
//        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < numSamples; i++) {
            VarAssignment assign = sampler.sample();
//            System.out.println("sample = " + assign);
            double x = assign.getContinuousVar(varName);
            sum += x;
//            min = Math.min(min, x);
//            max = Math.max(max, x);
        }
        double actualMean = sum / (double) numSamples;
//        System.out.println("actualMean = " + actualMean);
        return (actualMean - expectedMean) / (Math.sqrt(expectedVariance) / Math.sqrt(numSamples));
    }

    @Test
    public void basicTest2D() {
        XADD context = new XADD();
        int id1 = context.buildCanonicalXADDFromString("([N(x, 10, 666,3)])");//("([1.0 * x])");
        int id2 = context.buildCanonicalXADDFromString("([N(y, 10, 666,3)])");//("([1.0 * x])");
        int rootId = context.apply(id1, id2, XADD.PROD);
        XADD.XADDNode root = context._hmInt2Node.get(rootId);//("([N(n, 5, 666,3)])"));
        fixVarLimits(context, root, -10d, 20);

        context.getGraph(rootId).launchViewer("test");
        XaddVisualizer.visualize(root, "test", context);

        Sampler sampler = new GibbsSamplerWithCDFsPerSample(context, root);
        for (int i = 0; i < 50; i++) {
            VarAssignment assign = sampler.sample();
            System.out.println("t = " + assign);
        }
    }

    private void fixVarLimits(XADD context, XADD.XADDNode root, double varMin, double varMax) {
        HashSet<String> vars = root.collectVars();
        for (String var : vars) {
            context._hmMinVal.put(var, varMin);
            context._hmMaxVal.put(var, varMax);
        }
    }
}
