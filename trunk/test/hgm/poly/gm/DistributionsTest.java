package hgm.poly.gm;

import hgm.poly.Fraction;
import hgm.poly.Function;
import hgm.poly.PiecewiseExpression;
import hgm.poly.PolynomialFactory;
import hgm.poly.sampling.SamplingUtils;
import hgm.poly.sampling.frac.SymbolicFractionalJointGibbsSampler;
import hgm.poly.vis.FunctionVisualizer;
import hgm.sampling.VarAssignment;
import org.junit.Test;
import org.testng.Assert;

import java.util.Arrays;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 7/07/14
 * Time: 11:53 AM
 */
public class DistributionsTest {
    public static void main(String[] args) {
        DistributionsTest instance = new DistributionsTest();
        instance.testBowl();
    }
    @Test
    public void testCreateUniformDistribution() throws Exception {
        PolynomialFactory factory = new PolynomialFactory("x y z a_12 b c".split(" "));
        Distributions distBank = new Distributions(factory);
        PiecewiseExpression u = distBank.createUniformDistribution("x", factory.makePolynomial("2"), factory.makePolynomial("1*a_12^(2)*100*b^(3)"));
        System.out.println("u = " + u);
        Assert.assertTrue(1 == u.numCases());
    }

    @Test
    public void testBowl() {
        final String[] vars = PolynomialFactory.makeIndexedVars("v", 0, 1);
        final PolynomialFactory factory = new PolynomialFactory(vars);
        Distributions distBank = new Distributions(factory);
        int halfPow = 1;
        final PiecewiseExpression<Fraction> bowl = distBank.createBowlDistributionFraction(vars, 2, halfPow);
        System.out.println("bowl = " + bowl);
        FunctionVisualizer.visualize(new Function() {
            Double[] values = new Double[factory.getAllVars().length];

            @Override
            public double evaluate(VarAssignment fullVarAssign) {
                return bowl.evaluate(fullVarAssign.getContinuousVarAssignAsArray("v"));
            }

            @Override
            public String[] collectContinuousVars() {
                return vars;
            }
        }, -2.1, 2.1, 0.05, "");
    }

    @Test
    public void testOnBowlDistribution() throws Exception {
        int n = 3;
        String[] vars = PolynomialFactory.makeIndexedVars("x", 1, n);
        PolynomialFactory factory = new PolynomialFactory(vars);
        Distributions dBank = new Distributions(factory);

        int halfPow = 1;
        int radius = 1;
        PiecewiseExpression<Fraction> joint = dBank.createBowlDistributionFraction(vars, radius, halfPow);

        System.out.println("joint = " + joint);
        System.out.println("joint.getScopeVars() = " + joint.getScopeVars());
        System.out.println("joint.getFactory().getAllVars() = " + Arrays.toString(joint.getFactory().getAllVars()));

        //Sampler:
        SymbolicFractionalJointGibbsSampler sampler = SymbolicFractionalJointGibbsSampler.makeSampler(joint, -1.001, 1.001);
        for (int i = 0; i < 10; i++) {
            Map<String, Double> sample = sampler.reusableSampleAssignment();
            System.out.println("sample = " + sample);
        }

        int numSamples = 10000;
        String SAMPLES_FILE_PATH = "D:/JAVA/IdeaProjects/proj2/test/hgm/poly/gm/";
        SamplingUtils.save2DSamples(sampler, numSamples, SAMPLES_FILE_PATH + "scatter2D");
    }


}
