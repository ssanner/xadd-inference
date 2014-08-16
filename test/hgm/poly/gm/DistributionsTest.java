package hgm.poly.gm;

import hgm.poly.PiecewiseExpression;
import hgm.poly.PolynomialFactory;
import org.junit.Test;
import org.testng.Assert;

/**
 * Created by Hadi Afshar.
 * Date: 7/07/14
 * Time: 11:53 AM
 */
public class DistributionsTest {
    @Test
    public void testCreateUniformDistribution() throws Exception {
        PolynomialFactory factory = new PolynomialFactory("x y z a_12 b c".split(" "));
        Distributions distBank = new Distributions(factory);
        PiecewiseExpression u = distBank.createUniformDistribution("x", factory.makePolynomial("2"), factory.makePolynomial("1*a_12^(2)*100*b^(3)"));
        System.out.println("u = " + u);
        Assert.assertTrue(1 == u.numCases());
    }
}
