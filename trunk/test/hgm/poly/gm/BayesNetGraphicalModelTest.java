package hgm.poly.gm;

import hgm.poly.*;
import junit.framework.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 25/07/14
 * Time: 5:49 AM
 */
public class BayesNetGraphicalModelTest {
    @Test
    public void testAllInferenceRelevantFactors() throws Exception {
        PolynomialFactory factory = new PolynomialFactory("a b c d e f g h i".split(" "));
        Distributions dBank = new Distributions(factory);

        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();

        // a -> b -> c
        //   \____/
        PiecewiseExpression ap = dBank.createUniformDistribution("a", "-2", "2");
        PiecewiseExpression bp = dBank.createUniformDistribution("b", "-2*a^(1)", "2*a^(1)");
        Fraction cp = factory.makeFraction("1*a^(1) + 1*b^(1)"); //new PiecewisePolynomial(new ConstrainedPolynomial(factory.makePolynomial("1*a^(1) + 1*b^(1)"), new HashSet<Polynomial>()));

        bn.addFactor(new StochasticVAFactor("a", ap));
        bn.addFactor(new StochasticVAFactor("b", bp));
        bn.addFactor(new DeterministicFactor("c", cp));

        List<Factor> factors = bn.allInferenceRelevantFactors(Arrays.asList("a"));
        System.out.println("factors rel. a = " + factors);
        Assert.assertTrue(factors.size() == 1);

        factors = bn.allInferenceRelevantFactors(Arrays.asList("b"));
        System.out.println("factors rel. b = " + factors);
        Assert.assertTrue(factors.size() == 2);

        factors = bn.allInferenceRelevantFactors(Arrays.asList("c"));
        System.out.println("factors rel. c = " + factors);
        Assert.assertEquals(3, factors.size());
    }


}
