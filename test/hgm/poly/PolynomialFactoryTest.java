package hgm.poly;

import org.junit.Test;
import org.junit.Assert;

/**
 * Created by Hadi Afshar.
 * Date: 20/02/14
 * Time: 3:53 PM
 */
public class PolynomialFactoryTest {
    @Test
    public void testMakePolynomial() throws Exception {
        PolynomialFactory factory = new PolynomialFactory("v", "w", "x", "y", "z");
        Polynomial p1 = factory.makePolynomial("222 + 3*y^(0.0005)*z^(-3.8)*v^(1) + 5*x^(3)");
        Assert.assertEquals(p1.toString(), "3.0*v^(1.0)*y^(5.0E-4)*z^(-3.8)+222.0+5.0*x^(3.0)");

        Polynomial p2 = factory.makePolynomial(p1.toString());
        Assert.assertEquals(p2.toString(), p1.toString());

        Polynomial p3 = factory.makePolynomial("x^(2)*y^(2) + 3*y^(2)*x^(2)");
        Assert.assertEquals(p3.toString(), "4.0*x^(2.0)*y^(2.0)");
   }

}
