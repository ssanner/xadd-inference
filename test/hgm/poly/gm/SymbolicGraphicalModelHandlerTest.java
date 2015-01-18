package hgm.poly.gm;

import hgm.poly.*;
import hgm.poly.integral.frac.Digester;
import hgm.poly.reports.sg.ExperimentalGraphicalModels;
import hgm.poly.sampling.SamplerInterface;
import hgm.poly.sampling.SamplingUtils;
import hgm.poly.sampling.frac.*;

import junit.framework.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 25/07/14
 * Time: 8:16 PM
 */
public class SymbolicGraphicalModelHandlerTest {
    @Test
    public void testOnPolynomialsMakeJoint() throws Exception {
        PolynomialFactory factory = new PolynomialFactory("a b c d e f g h i".split(" "));
        Distributions dBank = new Distributions(factory);

        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();

        //        /--\------\  /--\
        // a -> b -> [c] -> [d] -> e
        //   \____/    \_________/
        PiecewiseExpression<Fraction> ap = dBank.createUniformDistributionFraction("a", "-2", "2");//dBank.createUniformDistribution("a", "-2", "2");
        PiecewiseExpression<Fraction> bp = dBank.createUniformDistributionFraction("b", "-2*a^(1)", "2*a^(1)");
        Fraction cp = factory.makeFraction("1*a^(1) + 1*b^(1)"); //new PiecewisePolynomial(new ConstrainedPolynomial(factory.makePolynomial("1*a^(1) + 1*b^(1)"), new HashSet<Polynomial>()));
        Fraction dp = factory.makeFraction("[1*c^(1)]/[b^(1) + c^(1)]"); //new PiecewisePolynomial(new ConstrainedPolynomial(factory.makePolynomial("1*a^(1) + 1*b^(1)"), new HashSet<Polynomial>()));
        PiecewiseExpression<Fraction> ep = dBank.createUniformDistributionFraction("e", "0", "1*d^(1)*c^(1)");

        bn.addFactor(new StochasticVAFactor("a", ap));
        bn.addFactor(new StochasticVAFactor("b", bp));
        bn.addFactor(new DeterministicFactor("c", cp));
        bn.addFactor(new DeterministicFactor("d", dp));
        bn.addFactor(new StochasticVAFactor("e", ep));

       /* List<Factor> factors = bn.allInferenceRelevantFactors(Arrays.asList("a"));
//        System.out.println("factors rel. a = " + factors);
        Assert.assertTrue(factors.size() == 1);

        factors = bn.allInferenceRelevantFactors(Arrays.asList("b"));
//        System.out.println("factors rel. b = " + factors);
        Assert.assertTrue(factors.size() == 2);

        factors = bn.allInferenceRelevantFactors(Arrays.asList("c"));
//        System.out.println("factors rel. c = " + factors);
        Assert.assertEquals(3, factors.size());*/

        SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
        Map<String, Double> evidence = new HashMap<String, Double>();
//        evidence.put("b", 3d); //("d", 3d);
        evidence.put("d", 3d);
        PiecewiseExpression<Fraction> joint = handler.makeJoint(bn, Arrays.asList("a e".split(" ")), evidence);

        //Digester:
        Digester digester = new Digester(joint, "e");
        System.out.println("digester.getInters() = " + digester.getInters());
    }

    @Test
    public void vintnerPrelude() {
        PolynomialFactory factory = new PolynomialFactory("a b c d e f g h i".split(" "));
        Distributions dBank = new Distributions(factory);

        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();

        // a -> b -> c -> d
        // \____\____\____\__> [e]
        PiecewiseExpression<Fraction> ap = dBank.createUniformDistributionFraction("a", "0", "10");
        PiecewiseExpression<Fraction> bp = dBank.createUniformDistributionFraction("b", "a^(1)", "10");
        PiecewiseExpression<Fraction> cp = dBank.createUniformDistributionFraction("c", "b^(1)", "10");
        PiecewiseExpression<Fraction> dp = dBank.createUniformDistributionFraction("d", "c^(1)", "10");
        Fraction ep = factory.makeFraction("[a^(1) + b^(1) + c^(1) + d^(1)]/[4]");

        bn.addFactor(new StochasticVAFactor("a", ap));
        bn.addFactor(new StochasticVAFactor("b", bp));
        bn.addFactor(new StochasticVAFactor("c", cp));
        bn.addFactor(new StochasticVAFactor("d", dp));
        bn.addFactor(new DeterministicFactor("e", ep));

        SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
        Map<String, Double> evidence = new HashMap<String, Double>();
        evidence.put("e", 10d);
        PiecewiseExpression<Fraction> joint = handler.makeJoint(bn, Arrays.asList("d".split(" ")), evidence);

        //Digester:
        Digester digester = new Digester(joint, "a");
        System.out.println("digester.getInters() = " + digester.getInters());

    }

    @Test
    public void testMomentum() throws Exception {
        PolynomialFactory factory = new PolynomialFactory("m_1 m_2 m v_1 v_2 v p_1 p_2 p".split(" "));
        Distributions dBank = new Distributions(factory);

        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();

        // m_1      v_1   m_2     v_2
        //   \__p_1__/    \__p_2__/
        //       \_____p______/
        PiecewiseExpression<Fraction> m1F = dBank.createUniformDistributionFraction("m_1", "0.1", "2.1");
        PiecewiseExpression<Fraction> m2F = dBank.createUniformDistributionFraction("m_2", "0.1", "2.1");
        PiecewiseExpression<Fraction> v_1F = dBank.createUniformDistributionFraction("v_1", "-2", "2");
        PiecewiseExpression<Fraction> v_2F = dBank.createUniformDistributionFraction("v_2", "-2", "v_1^(1)");
//        Fraction mF = factory.makeFraction("1*m_1^(1) + 1*m_2^(1)");
        Fraction pF = factory.makeFraction("m_1^(1)*v_1^(1) + m_2^(1)*v_2^(1)");
//        Fraction vF = factory.makeFraction("[p^(1)]/[m^(1)]");

        bn.addFactor(new StochasticVAFactor("m_1", m1F)); //mass 1
        bn.addFactor(new StochasticVAFactor("m_2", m2F)); //mass 2
        bn.addFactor(new StochasticVAFactor("v_1", v_1F)); //mass 2
        bn.addFactor(new StochasticVAFactor("v_2", v_2F)); //mass 2
//        bn.addFactor(new DeterministicFactor("m", mF)); //total mass
        bn.addFactor(new DeterministicFactor("p", pF)); //total momentum = p1 + p2 = m1*v1 + m2*v2 = (m1 + m2)*v
//        bn.addFactor(new DeterministicFactor("v", vF)); //total velocity (after collision)

       /* List<Factor> factors = bn.allInferenceRelevantFactors(Arrays.asList("a"));
//        System.out.println("factors rel. a = " + factors);
        Assert.assertTrue(factors.size() == 1);

        factors = bn.allInferenceRelevantFactors(Arrays.asList("b"));
//        System.out.println("factors rel. b = " + factors);
        Assert.assertTrue(factors.size() == 2);

        factors = bn.allInferenceRelevantFactors(Arrays.asList("c"));
//        System.out.println("factors rel. c = " + factors);
        Assert.assertEquals(3, factors.size());*/

        SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
        Map<String, Double> evidence = new HashMap<String, Double>();
        evidence.put("p", 3d);
        PiecewiseExpression<Fraction> joint = handler.makeJoint(bn, Arrays.asList("m_1 v_1".split(" ")), evidence);
        System.out.println("joint.getScopeVars() = " + joint.getScopeVars());

        //Digester:
//        Digester digester = new Digester(joint, "v_1");
//        System.out.println("digester.getInters() = " + digester.getInters());

        //Sampler:
        FractionalJointSymbolicGibbsSampler sampler = FractionalJointSymbolicGibbsSampler.makeSampler(joint, -10, 10);
        for (int i = 0; i < 10; i++) {
            Double[] sample = sampler.reusableSample();
            System.out.println("sample = " + Arrays.toString(sample));
        }
        System.out.println("\n");

        for (int i = 0; i < 10; i++) {
            Map<String, Double> sample = sampler.reusableSampleAssignment();
            System.out.println("sample = " + sample);
        }

        System.out.println("----------------------------");

        SamplerInterface sampler2 = handler.makeQuerySampler(bn, Arrays.asList("m_1 v_1 m_2 v_2".split(" ")), evidence, -10, 10, FractionalJointSymbolicGibbsSampler.makeJointToSampler());
        for (int i = 0; i < 10; i++) {
            Double[] sample = sampler2.reusableSample();
            System.out.println("sample = " + Arrays.toString(sample));
            double m_1 = sample[0];
            double v_1 = sample[1];
            double m_2 = sample[2];
            double v_2 = sample[3];
            System.out.println("(m_1*v_1 + m_2*v_2) = " + (m_1 * v_1 + m_2 * v_2));
        }
    }

    @Test
    public void testMomentum2() throws Exception {
        PolynomialFactory factory = new PolynomialFactory("m_1 m_2 m v_1 v_2 v p_1 p_2 p".split(" "));
        Distributions dBank = new Distributions(factory);

        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();

        Double mu1 = 0.1;
        Double mu2 = 2.1;
        Double nu1 = -2.0;
        Double nu2 = 2.0;
        boolean symmetric = false;


        // m_1      v_1   m_2     v_2
        //   \__p_1__/    \__p_2__/
        //       \_____p______/
        PiecewiseExpression<Fraction> m1F = dBank.createUniformDistributionFraction("m_1", mu1.toString(), mu2.toString());
        PiecewiseExpression<Fraction> m2F = dBank.createUniformDistributionFraction("m_2", mu1.toString(), mu2.toString());
        PiecewiseExpression<Fraction> v_1F = dBank.createUniformDistributionFraction("v_1", nu1.toString(), nu2.toString());
        PiecewiseExpression<Fraction> v_2F = dBank.createUniformDistributionFraction("v_2", nu1.toString(), symmetric? nu2.toString(): "v_1^(1)");
//        Fraction mF = factory.makeFraction("1*m_1^(1) + 1*m_2^(1)");
        Fraction pF = factory.makeFraction("m_1^(1)*v_1^(1) + m_2^(1)*v_2^(1)");
//        Fraction vF = factory.makeFraction("[p^(1)]/[m^(1)]");

        bn.addFactor(new StochasticVAFactor("m_1", m1F)); //mass 1
        bn.addFactor(new StochasticVAFactor("m_2", m2F)); //mass 2
        bn.addFactor(new StochasticVAFactor("v_1", v_1F)); //mass 2
        bn.addFactor(new StochasticVAFactor("v_2", v_2F)); //mass 2
//        bn.addFactor(new DeterministicFactor("m", mF)); //total mass
        bn.addFactor(new DeterministicFactor("p", pF)); //total momentum = p1 + p2 = m1*v1 + m2*v2 = (m1 + m2)*v
//        bn.addFactor(new DeterministicFactor("v", vF)); //total velocity (after collision)


        SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
        Map<String, Double> evidence = new HashMap<String, Double>();
        evidence.put("p", 3d);
//        evidence.put("m_1", 2d);
        evidence.put("v_2", 0.2d);

        SamplerInterface sampler = handler.makeQuerySampler(bn, Arrays.asList("m_1 v_1".split(" ")), evidence, -10, 10,
//                FractionalJointBaselineGibbsSampler.makeJointToSampler()
                FractionalJointRejectionSampler.makeJointToSampler(1)
//                FractionalJointSelfTunedMetropolisHastingSampler.makeJointToSampler(0.2, 40, 100)
//                FractionalJointMetropolisHastingSampler.makeJointToSampler(10)
//                FractionalJointSymbolicGibbsSampler.makeJointToSampler()
        );
//        for (int i = 0; i<10; i++) {
//            Double[] sample = sampler.reusableSample();
//            System.out.println("sample = " + Arrays.toString(sample));
//            double m_1 = sample[0];
//            double v_1 = sample[1];
//        }
//            double m_2 = sample[2];
//            double v_2 = sample[3];
//            System.out.println("(m_1*v_1 + m_2*v_2) = " + (m_1 * v_1 + m_2 * v_2));

        int numSamples = 1000;
       /* for (int i = 0; i < numSamples; i++) {
            Double[] s = sampler.reusableSample();
            System.out.println(i + ". sample = " + Arrays.toString(s));
            Assert.assertTrue(s[1]<s[0]);
        }*/
        String SAMPLES_FILE_PATH = "D:/JAVA/IdeaProjects/proj2/test/hgm/poly/gm/";
        SamplingUtils.save2DSamples(sampler, numSamples, SAMPLES_FILE_PATH + "scatter2D_gibbs");

    }

    @Test
    public void testMomentum3(){
        int param = 2;
        Double muAlpha = -2.9;   //todo why negative does not work?     IMPORTANT BUG to be solved
        Double muBeta = 2.2;
        Double nuAlpha = muAlpha;
        Double nuBeta = muBeta;
        boolean symmetric = true;

//        GraphicalModel bn = ExperimentalGraphicalModels.makeCollisionModel(param, muAlpha, muBeta, nuAlpha, nuBeta, symmetric);//paramDataCount2DataGenerator.createJointGenerator(param);

        //*************************************************************************************************
        // m_1      v_1   m_2     v_2
        //   \__p_1__/    \__p_2__/
        //       \_____p______/
        PolynomialFactory factory = new PolynomialFactory("m_1 m_2 v_1 v_2 p_t".split(" "));
        Distributions dBank = new Distributions(factory);

        PiecewiseExpression<Fraction> m1F = dBank.createUniformDistributionFraction("m_1", muAlpha.toString(), muBeta.toString());
        PiecewiseExpression<Fraction> m2F = dBank.createUniformDistributionFraction("m_2", muAlpha.toString(), muBeta.toString());
        PiecewiseExpression<Fraction> v_1F = dBank.createUniformDistributionFraction("v_1", nuAlpha.toString(), nuBeta.toString());
        PiecewiseExpression<Fraction> v_2F = dBank.createUniformDistributionFraction("v_2", nuAlpha.toString(), symmetric? nuBeta.toString(): "v_1^(1)");
//        Fraction pF = factory.makeFraction("m_1^(1)*v_1^(1) + m_2^(1)*v_2^(1)");
        Fraction pF = factory.makeFraction("m_1^(1)*v_1^(1) + m_2^(1)*v_2^(1)");

        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();
        bn.addFactor(new StochasticVAFactor("m_1", m1F)); //mass 1
        bn.addFactor(new StochasticVAFactor("m_2", m2F)); //mass 2
        bn.addFactor(new StochasticVAFactor("v_1", v_1F)); //mass 2
        bn.addFactor(new StochasticVAFactor("v_2", v_2F)); //mass 2
        bn.addFactor(new DeterministicFactor("p_t", pF)); //total momentum = p1 + p2 = m1*v1 + m2*v2 = (m1 + m2)*v
        //*************************************************************************************************


        SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
        Map<String, Double> evidence = new HashMap<String, Double>();
        double p = 1.5 * param;
        evidence.put("p_t", p);
//        evidence.put("m_1", 2d);
//        evidence.put("v_2", 0.2d);

        String queryStr = "";
        for (int i = 0; i < param; i++) {
            queryStr += "v_" + (i+1) +" ";
        }
        SamplerInterface sampler = handler.makeQuerySampler(bn,
                Arrays.asList(queryStr.trim().split(" ")),
//                Arrays.asList("v_1 v_2".split(" ")),
                evidence, -5, 5,
//                FractionalJointBaselineGibbsSampler.makeJointToSampler()
                FractionalJointSymbolicGibbsSampler.makeJointToSampler()
//                FractionalJointRejectionSampler.makeJointToSampler(10)
//                FractionalJointSelfTunedMetropolisHastingSampler.makeJointToSampler(1, 50, 1000)
//                FractionalJointMetropolisHastingSampler.makeJointToSampler(0.1)
        );

        int numSamples = 1000000;
        double[] average = new double[param]; //Assuming the size of Query = num. params
        for (int i = 0; i < numSamples; i++) {
            Double[] s = sampler.reusableSample();

            for (int j=0; j<average.length;j++) {
                average[j]+= s[j];
            }
//            System.out.println(i + ". sample = " + Arrays.toString(s));

        }

        for (int j=0; j<average.length;j++) {
            average[j]/= numSamples;
        }

        double m = Math.pow(p / param, 0.5);
        System.out.println("m = " + m);
        System.out.println("average = " + Arrays.toString(average));
//        String SAMPLES_FILE_PATH = "D:/JAVA/IdeaProjects/proj2/test/hgm/poly/gm/";
//        SamplingUtils.save2DSamples(sampler, numSamples, SAMPLES_FILE_PATH + "scatter2D");
    }


    @Test
    public void vintnerPrelude2() throws FileNotFoundException {
        PolynomialFactory factory = new PolynomialFactory("a b c d e f g h i".split(" "));
        Distributions dBank = new Distributions(factory);

        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();

        // a -> b -> c -> d
        // \____\____\____\__> [e]
        PiecewiseExpression<Fraction> ap = dBank.createUniformDistributionFraction("a", "0", "14");
        PiecewiseExpression<Fraction> bp = dBank.createUniformDistributionFraction("b", "a^(1)", "14");
        PiecewiseExpression<Fraction> cp = dBank.createUniformDistributionFraction("c", "b^(1)", "14");
        PiecewiseExpression<Fraction> dp = dBank.createUniformDistributionFraction("d", "c^(1)", "14");
//        Fraction ep = factory.makeFraction("[a^(1) + b^(1) + c^(1) + d^(1)]/[4]");
        Fraction ep = factory.makeFraction("[b^(1) + c^(1) + d^(1)]/[3]");


        bn.addFactor(new StochasticVAFactor("a", ap));
        bn.addFactor(new StochasticVAFactor("b", bp));
        bn.addFactor(new StochasticVAFactor("c", cp));
        bn.addFactor(new StochasticVAFactor("d", dp));
        bn.addFactor(new DeterministicFactor("e", ep));

        SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
        Map<String, Double> evidence = new HashMap<String, Double>();
        evidence.put("e", 10d);
        /*PiecewiseExpression<Fraction> joint = handler.makeJoint(bn, Arrays.asList("d".split(" ")), evidence);
        System.out.println("joint = " + joint);

        //Digester:
        Digester digester = new Digester(joint, "a");
        System.out.println("digester.getInters() = " + digester.getInters());*/


        SamplerInterface sampler = handler.makeQuerySampler(bn, Arrays.asList("a d".split(" ")), evidence, -20, 20, FractionalJointSymbolicGibbsSampler.makeJointToSampler());

        int numSamples = 10000;
        for (int i = 0; i < numSamples; i++) {
            Double[] s = sampler.reusableSample();
            System.out.println(i + ". sample = " + Arrays.toString(s));
        }
        String SAMPLES_FILE_PATH = "D:/JAVA/IdeaProjects/proj2/test/hgm/poly/gm/";
        SamplingUtils.save2DSamples(sampler, numSamples, SAMPLES_FILE_PATH + "scatter2D");
    }

    @Test
    public void testTrivial() throws Exception {
        PolynomialFactory factory = new PolynomialFactory("a b c d w x y z".split(" "));
        Distributions dBank = new Distributions(factory);

        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();

        PiecewiseExpression<Fraction> aF = dBank.createUniformDistributionFraction("a", "0", "10");
        PiecewiseExpression<Fraction> bF = dBank.createUniformDistributionFraction("b", "0", "a^(1)");

        bn.addFactor(new StochasticVAFactor("a", aF));
        bn.addFactor(new StochasticVAFactor("b", bF));
        bn.addFactor(new DeterministicFactor("z", factory.makeFraction("a^(1) + b^(1)")));

        SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
        Map<String, Double> evidence = new HashMap<String, Double>();
        evidence.put("z", 3d);
        PiecewiseExpression<Fraction> joint = handler.makeJoint(bn, Arrays.asList("a b".split(" ")), evidence);
        System.out.println("joint = " + joint);
        System.out.println("joint.getScopeVars() = " + joint.getScopeVars());
        System.out.println("joint.getFactory().getAllVars() = " + Arrays.toString(joint.getFactory().getAllVars()));

        //Digester:
//        Digester digester = new Digester(joint, "v_1");
//        System.out.println("digester.getInters() = " + digester.getInters());

        //Sampler:
        FractionalJointSymbolicGibbsSampler sampler = FractionalJointSymbolicGibbsSampler.makeSampler(joint, -10, 20);
        for (int i = 0; i < 100; i++) {
            Map<String, Double> sample = sampler.reusableSampleAssignment();
//            Double[] sample = sampler.reusableSample();
//            Assert.assertTrue(sample.get("b")>0);
//            Assert.assertTrue(sample.get("b")<10);
//            Assert.assertTrue(sample[1]>0);
//            Assert.assertTrue(sample[1]<sample[0]);

            System.out.println("sample = " + sample);
        }
    }

    @Test
    public void testCorrectness1() throws Exception {
        PolynomialFactory factory = new PolynomialFactory("x y n p".split(" "));
        Distributions dBank = new Distributions(factory);

        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();

        PiecewiseExpression<Fraction> xF = dBank.createUniformDistributionFraction("x", "0", "1");
        PiecewiseExpression<Fraction> yF = dBank.createUniformDistributionFraction("y", "0", "1");
        Fraction pF = factory.makeFraction("y^(1) + -1*x^(4)");

        bn.addFactor(new StochasticVAFactor("x", xF)); //mass 1
        bn.addFactor(new StochasticVAFactor("y", yF)); //mass 2
        bn.addFactor(new DeterministicFactor("p", pF)); //total momentum = p1 + p2 = m1*v1 + m2*v2 = (m1 + m2)*v

        SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
        Map<String, Double> evidence = new HashMap<String, Double>();
        evidence.put("p", 0d);

        SamplerInterface sampler = handler.makeQuerySampler(bn, Arrays.asList("x y".split(" ")), evidence, -2, 2,
                FractionalJointRejectionSampler.makeJointToSampler(2)
//                FractionalJointBaselineGibbsSampler.makeJointToSampler()
        );

        int numSamples = 100;
       /* for (int i = 0; i < numSamples; i++) {
            Double[] s = sampler.reusableSample();
            System.out.println(i + ". sample = " + Arrays.toString(s));
            Assert.assertTrue(s[1]<s[0]);
        }*/
        String SAMPLES_FILE_PATH = "D:/JAVA/IdeaProjects/proj2/test/hgm/poly/gm/";
        SamplingUtils.save2DSamples(sampler, numSamples, SAMPLES_FILE_PATH + "scatter2D");
    }

    @Test
    public void testCorrectnessWithNoise() throws Exception {
        PolynomialFactory factory = new PolynomialFactory("a b c x y n p".split(" "));
        Distributions dBank = new Distributions(factory);

        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();

        PiecewiseExpression<Fraction> xF = dBank.createUniformDistributionFraction("x", "0", "1");
        PiecewiseExpression<Fraction> yF = dBank.createUniformDistributionFraction("y", "0", "1");
        Fraction pF = factory.makeFraction("y^(1) + -1*x^(4)");
        PiecewiseExpression<Fraction> nF = dBank.createNonNormalizedBellShapedDistribution("n", "p^(1) + -0.01", "p^(1) + 0.01");

        bn.addFactor(new StochasticVAFactor("x", xF)); //mass 1
        bn.addFactor(new StochasticVAFactor("y", yF)); //mass 2
        bn.addFactor(new DeterministicFactor("p", pF)); //total momentum = p1 + p2 = m1*v1 + m2*v2 = (m1 + m2)*v
        bn.addFactor(new StochasticVAFactor("n", nF));

        SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
        Map<String, Double> evidence = new HashMap<String, Double>();
        evidence.put("n", 0.0d);

        SamplerInterface sampler = handler.makeQuerySampler(bn, Arrays.asList("x y".split(" ")), evidence, 0, 1,
                FractionalJointRejectionSampler.makeJointToSampler(2)
//                FractionalJointBaselineGibbsSampler.makeJointToSampler()
        );

        int numSamples = 1000;
        for (int i = 0; i < numSamples; i++) {
            Double[] s = sampler.reusableSample();
            System.out.println(i + ". sample = " + Arrays.toString(s));
        }
        String SAMPLES_FILE_PATH = "D:/JAVA/IdeaProjects/proj2/test/hgm/poly/gm/";
        SamplingUtils.save2DSamples(sampler, numSamples, SAMPLES_FILE_PATH + "scatter2D");
    }


}
