package hgm.poly.sampling;

import hgm.poly.ConstrainedPolynomial;
import hgm.poly.PolynomialFactory;
import hgm.poly.vis.PolynomialVisualizer;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

/**
 * Created by Hadi Afshar.
 * Date: 21/02/14
 * Time: 2:34 PM
 */
public class PolyGibbsSamplerTest {
    public static String SAMPLES_FILE_NAME = "D:\\JAVA\\IdeaProjects\\proj2\\test\\hgm\\poly\\sampling\\scatter2D.txt";

    public static void main(String[] args) throws Exception {
        PolyGibbsSamplerTest instance = new PolyGibbsSamplerTest();
//        instance.testUniformInTriangle();
//        instance.testLinearInTriangle();
//        instance.testUniformInRing();
        instance.testUniformInChewedRing();
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

        PolynomialVisualizer.visualize(cp, minVarLim, maxVarLim, 0.5, "func");

        PolyGibbsSampler sampler = PolyGibbsSampler.makeGibbsSampler(cp, minVarLim, maxVarLim, null);

        save2DSamples(sampler, numSamples);

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
        ConstrainedPolynomial cp = factory.makeConstrainedPolynomial("x^(2)+-10*x^(1)+y^(2)", cond1, cond2, cond3);

        double minVarLim = -30d;
        double maxVarLim = 30d;

        PolynomialVisualizer.visualize(cp, minVarLim, maxVarLim, 0.5, "func");

        PolyGibbsSampler sampler = PolyGibbsSampler.makeGibbsSampler(cp, minVarLim, maxVarLim, null);

        save2DSamples(sampler, numSamples);

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

        PolynomialVisualizer.visualize(cp, minVarLim, maxVarLim, 0.5, "func");

        PolyGibbsSampler sampler = PolyGibbsSampler.makeGibbsSampler(cp, minVarLim, maxVarLim, null);

        save2DSamples(sampler, numSamples);

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

        PolynomialVisualizer.visualize(cp, minVarLim, maxVarLim, 0.5, "func");

        PolyGibbsSampler sampler = PolyGibbsSampler.makeGibbsSampler(cp, minVarLim, maxVarLim, null);

        save2DSamples(sampler, numSamples);

    }

    @Test
    public void testUniformInChewedRing() throws Exception {
        int numSamples = 10000;

        double a = 10d;
        double b = 0d;
        double r = 20;
         double twoA = 2*a;
         double twoB = 2*b;
        double a2b2 = a*a + b*b;

        PolynomialFactory factory = new PolynomialFactory("x", "y");
        String cond1 = "3*x^(2)+y^(2)+-20>0";
        String cond2 = "x^(2)+y^(2)+-150<0";
        String cond3 = "x^(2)+-" + twoA + "*x^(1)+y^(2)+-"+twoB+"*y^(1)+" + a2b2 + "+-"+r+">0";
        ConstrainedPolynomial cp = factory.makeConstrainedPolynomial("1", cond1, cond2,cond3);

        double minVarLim = -30d;
        double maxVarLim = 30d;

        PolynomialVisualizer.visualize(cp, minVarLim, maxVarLim, 0.5, "func");

        PolyGibbsSampler sampler = PolyGibbsSampler.makeGibbsSampler(cp, minVarLim, maxVarLim, null);

        save2DSamples(sampler, numSamples);

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

        PolynomialVisualizer.visualize(cp, minVarLim, maxVarLim, 0.5, "func");

        PolyGibbsSampler sampler = PolyGibbsSampler.makeGibbsSampler(cp, minVarLim, maxVarLim, null);

        save2DSamples(sampler, numSamples);

    }

    public static void save2DSamples(PolyGibbsSampler sampler, int numSamples) throws FileNotFoundException {
        PrintStream ps;

        ps = new PrintStream(new FileOutputStream(SAMPLES_FILE_NAME));

        for (int i = 0; i < numSamples; i++) {
            Double[] sample = sampler.sample();
            System.out.println("sample = " + Arrays.toString(sample));
            ps.println(sample[0] + "\t" + sample[1]);

        }

        ps.close();
    }

    public static void testStatistics(String varName, int numSamples, PolyGibbsSampler sampler,
                                      double min, double max, double mean, double variance, double epsilon) {
        Integer varIndex = sampler.cp.getPolynomialFactory().getVarIndex(varName);
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
