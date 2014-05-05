package hgm.poly.sampling;

import hgm.poly.ConstrainedPolynomial;
import hgm.poly.PolynomialFactory;
import hgm.poly.pref.*;
import hgm.poly.vis.FunctionVisualizer;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Hadi Afshar.
 * Date: 21/02/14
 * Time: 2:34 PM
 */

//TODO NOTE: this is a fast (and incomplete) hack of the other class to test Symbolic and other multi-polytope samplers...
public class AbstractPolytopesSamplerTest {
    public static String SAMPLES_FILE_PATH = "D:/JAVA/IdeaProjects/proj2/test/hgm/poly/sampling/";

    public static void main(String[] args) throws Exception {
        AbstractPolytopesSamplerTest instance = new AbstractPolytopesSamplerTest();
//        instance.testUniformInTriangle();
        instance.testLinearInTriangle();
//        instance.testUniformInRing();
//        instance.testUniformInChewedRing();
//        instance.testQuadraticInTriangle();
    }

    @Test
    public void testUniformInTriangle() throws Exception {
        //  c   /\
        //     / |\
        //  0 /  | \
        //   a   0  b

        double a = -30.0;
        double b = 10.0;  //note: c=0
        double w = -b / a;
        int numSamples = 10000;
        double epsilon = 0.9;

        PolynomialFactory factory = new PolynomialFactory("x", "y");
        String cond1 = "x^(1)>0".replaceAll("B", "" + b).replaceAll("W", "" + w);
        String cond2 = "x^(1)+y^(1)+-B<0".replaceAll("B", "" + b).replaceAll("W", "" + w);
        String cond3 = "x^(1)+-W*y^(1)+-B<0".replaceAll("B", "" + b).replaceAll("W", "" + w);
        ConstrainedPolynomial cp = factory.makeConstrainedPolynomial("1", cond1, cond2, cond3);

        /*String cond1 = "x^(1)+10>0";
        String cond2 = "x^(1)+-10<0";
        String cond3 = "y^(1)+20>0";
        String cond4 = "y^(1)+-2<0";
        ConstrainedPolynomial cp = factory.makeConstrainedPolynomial("1", cond1, cond2, cond3, cond4);*/

//        int nodeId = context.buildCanonicalXADDFromString(
//                ("([1*x > 0] " +
//                        "([1*x + 1*y < B] " +
//                        "       ([1*x - W*y < B] ([1]) ([0])) " +
//                        "       ([0]) ) " +
//                        "([0]))").replaceAll("B", "" + b).replaceAll("W", ""+ w));

        double minVarLim = -30d;
        double maxVarLim = 30d;

        FunctionVisualizer.visualize(cp, minVarLim, maxVarLim, 0.5, "func");

        AbstractPolytopesSampler sampler = SymbolicGibbsPolytopesSampler.makeSampler(
                new PosteriorHandler(cp.getPolynomialFactory(), cp, 0, 100), minVarLim, maxVarLim, null);

        SamplingUtils.save2DSamples(sampler, numSamples, SAMPLES_FILE_PATH + "scatterGibbs");//"scatter2D");

        // the projection of the distribution on the Y axis is a triangular distribution:
        double c = 0;
        testStatistics("y", numSamples, sampler,
                a, b, (a + b + c) / 3.0, (a * a + b * b + c * c - a * b - a * c - b * c) / 18.0, epsilon);
    }

    //todo does not work well... generate samples out of the region....
    @Test
    public void testQuadraticInTriangle() throws Exception {
        //  c   /\
        //     / |\
        //  0 /  | \
        //   a   0  b

        double a = -30.0;
        double b = 10.0;  //note: c=0
        double w = -b / a;
        int numSamples = 10000;

        PolynomialFactory factory = new PolynomialFactory("x", "y");
        String cond1 = "x^(1)>0".replaceAll("B", "" + b).replaceAll("W", "" + w);
        String cond2 = "x^(1)+y^(1)+-B<0".replaceAll("B", "" + b).replaceAll("W", "" + w);
        String cond3 = "x^(1)+-W*y^(1)+-B<0".replaceAll("B", "" + b).replaceAll("W", "" + w);
        ConstrainedPolynomial cp = factory.makeConstrainedPolynomial(
//                "x^(2)+-10*x^(1)+y^(2)"
                "x^(2)+-10*x^(1)+25+y^(2)"
                , cond1, cond2, cond3);

        double minVarLim = -30d;
        double maxVarLim = 30d;

        FunctionVisualizer.visualize(cp, minVarLim, maxVarLim, 0.5, "func");

        AbstractPolytopesSampler sampler =
                SymbolicGibbsPolytopesSampler.makeSampler(new PosteriorHandler(cp.getPolynomialFactory(), cp, 0, 100), minVarLim, maxVarLim, null);
//        GatedGibbsPolytopesSampler.makeSampler(new PolytopesHandler(cp.getPolynomialFactory(), cp, 0, 100), minVarLim, maxVarLim, null);

        SamplingUtils.save2DSamples(sampler, numSamples, SAMPLES_FILE_PATH + "scatter2D");

    }

    @Test
    public void testLinearInTriangle() throws Exception {
        //  c   /\
        //     / |\
        //  0 /  | \
        //   a   0  b

        double a = -30.0;
        double b = 10.0;  //note: c=0
        double w = -b / a;
        int numSamples = 10000;

        PolynomialFactory factory = new PolynomialFactory("x", "y");
        String cond1 = "x^(1)>0".replaceAll("B", "" + b).replaceAll("W", "" + w);
        String cond2 = "x^(1)+y^(1)+-B<0".replaceAll("B", "" + b).replaceAll("W", "" + w);
        String cond3 = "x^(1)+-W*y^(1)+-B<0".replaceAll("B", "" + b).replaceAll("W", "" + w);
        ConstrainedPolynomial cp = factory.makeConstrainedPolynomial("x^(1)", cond1, cond2, cond3);

        double minVarLim = -30d;
        double maxVarLim = 30d;

        FunctionVisualizer.visualize(cp, minVarLim, maxVarLim, 0.5, "func");

        AbstractPolytopesSampler sampler = SymbolicGibbsPolytopesSampler.makeSampler(new PosteriorHandler(cp.getPolynomialFactory(), cp, 0, 100), minVarLim, maxVarLim, null);

        SamplingUtils.save2DSamples(sampler, numSamples, SAMPLES_FILE_PATH + "scatter2D");

    }

    @Test
    public void testUniformInRing() throws Exception {
        int numSamples = 10000;

        PolynomialFactory factory = new PolynomialFactory("x", "y");
        String cond1 = "x^(2)+y^(2)+-20>0";
        String cond2 = "x^(2)+y^(2)+-150<0";
        ConstrainedPolynomial cp = factory.makeConstrainedPolynomial("1", cond1, cond2);

        double minVarLim = -30d;
        double maxVarLim = 30d;

        FunctionVisualizer.visualize(cp, minVarLim, maxVarLim, 0.5, "func");

        PolyGibbsSampler sampler = PolyGibbsSampler.makeGibbsSampler(cp, minVarLim, maxVarLim, null);

        SamplingUtils.save2DSamples(sampler, numSamples, SAMPLES_FILE_PATH + "scatter2D");

    }

    @Test
    public void testUniformInChewedRing() throws Exception {
        int numSamples = 10000;

        double a = 10d;
        double b = 0d;
        double r = 20;
        double twoA = 2 * a;
        double twoB = 2 * b;
        double a2b2 = a * a + b * b;

        PolynomialFactory factory = new PolynomialFactory("x", "y");
        String cond1 = "3*x^(2)+y^(2)+-20>0";
        String cond2 = "x^(2)+y^(2)+-150<0";
        String cond3 = "x^(2)+-" + twoA + "*x^(1)+y^(2)+-" + twoB + "*y^(1)+" + a2b2 + "+-" + r + ">0";
        ConstrainedPolynomial cp = factory.makeConstrainedPolynomial("1", cond1, cond2, cond3);

        double minVarLim = -30d;
        double maxVarLim = 30d;

        FunctionVisualizer.visualize(cp, minVarLim, maxVarLim, 0.5, "func");

        PolyGibbsSampler sampler = PolyGibbsSampler.makeGibbsSampler(cp, minVarLim, maxVarLim, null);

        SamplingUtils.save2DSamples(sampler, numSamples, SAMPLES_FILE_PATH + "scatter2D");

    }

    @Test
    public void testQuadraticInRing() throws Exception {
        int numSamples = 10000;

        PolynomialFactory factory = new PolynomialFactory("x", "y");
        String cond1 = "x^(2)+y^(2)+-20>0";
        String cond2 = "x^(2)+y^(2)+-50<0";
        ConstrainedPolynomial cp = factory.makeConstrainedPolynomial("x^(2)", cond1, cond2);

        double minVarLim = -30d;
        double maxVarLim = 30d;

        FunctionVisualizer.visualize(cp, minVarLim, maxVarLim, 0.5, "func");

        PolyGibbsSampler sampler = PolyGibbsSampler.makeGibbsSampler(cp, minVarLim, maxVarLim, null);

        SamplingUtils.save2DSamples(sampler, numSamples, SAMPLES_FILE_PATH + "scatter2D");

    }


    public static void testStatistics(String varName, int numSamples, AbstractPolytopesSampler sampler,
                                      double min, double max, double mean, double variance, double epsilon) {
        Integer varIndex = sampler.getFactory().getVarIndex(varName);
        //actual...
        double aMin = Double.POSITIVE_INFINITY;
        double aMax = Double.NEGATIVE_INFINITY;
        double aMean = 0.0;
        double aVariance = 0;

        for (int i = 0; i < numSamples; i++) {
            Double[] assign = sampler.sample();

//            System.out.println("sample = " + assign);
            double x = assign[varIndex];
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

}
