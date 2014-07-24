package hgm.poly;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 20/02/14
 * Time: 11:49 PM
 */
public class PolynomialTest {
    @Test
    public void testAddToThis() throws Exception {
        PolynomialFactory factory = new PolynomialFactory("v", "w", "x", "y", "z");
        Polynomial p1 = factory.makePolynomial("x^(2)*y^(2) + 3*y^(2)*x^(2) + 4*x^(2)");
        Polynomial p2 = factory.makePolynomial("4*x^(2)*y^(2) + 3*y^(3)*x^(2) + -4*x^(2)");

        String p1Str = "4.0*x^(2.0)*y^(2.0)+4.0*x^(2.0)";
        Assert.assertEquals(p1.toString(), p1Str);

        Polynomial p3 = p1.clone();
        p3.addToThis(p2);
        Assert.assertEquals(p3.toString(), "8.0*x^(2.0)*y^(2.0)+3.0*x^(2.0)*y^(3.0)");

        //p1 should not be changed
        Assert.assertEquals(p1.toString(), p1Str);

    }

    @Test
    public void testMultiplyScalarInThis() throws Exception {
        PolynomialFactory factory = new PolynomialFactory("v", "w", "x", "y", "z");
        Polynomial p1 = factory.makePolynomial("z^(2)*y^(2) + 3*y^(2)*x^(2) + 4*x^(2)");
        p1.multiplyScalarInThis(10);
        Assert.assertEquals("30.0*x^(2.0)*y^(2.0)+10.0*y^(2.0)*z^(2.0)+40.0*x^(2.0)", p1.toString());

    }

    @Test
    public void testMultiply() throws Exception {
        PolynomialFactory factory = new PolynomialFactory("v", "w", "x", "y", "z");
        Polynomial p1 = factory.makePolynomial("x^(2)*y^(2) + 3*x^(2) + 5*y^(2)");
        Polynomial p2 = p1.multiply(factory.makePolynomial("10*x^(10)"));
        Assert.assertEquals("10.0*x^(12.0)*y^(2.0)+30.0*x^(12.0)+50.0*x^(10.0)*y^(2.0)", p2.toString());
        Polynomial p3 = p1.multiply(factory.makePolynomial("1000 + 2*x^(1)"));
        Assert.assertEquals("1000.0*x^(2.0)*y^(2.0)+2.0*x^(3.0)*y^(2.0)+3000.0*x^(2.0)+5000.0*y^(2.0)+10.0*x^(1.0)*y^(2.0)+6.0*x^(3.0)", p3.toString());
        Polynomial p4 = p1.multiply(factory.makePolynomial("10*x^(1)*v^(1)*w^(1)"));
        Assert.assertEquals("50.0*v^(1.0)*w^(1.0)*x^(1.0)*y^(2.0)+10.0*v^(1.0)*w^(1.0)*x^(3.0)*y^(2.0)+30.0*v^(1.0)*w^(1.0)*x^(3.0)", p4.toString());
    }

    @Test
    public void testSubstitute() throws Exception {
        PolynomialFactory factory = new PolynomialFactory("v", "w", "x", "y");
        Polynomial p1 = factory.makePolynomial("x^(2)*y^(2) + 3*x^(2) + 5*y^(2)");
        Polynomial p2 = p1.substitute(new Double[]{1d, 1d, 1d, 2d});
        Assert.assertEquals("27.0", p2.toString());

        Polynomial p3 = p1.substitute(new Double[]{1d, 1d, 10d, null});
        Assert.assertEquals("105.0*y^(2.0)+300.0", p3.toString());

        Map<String, Double> assign = new HashMap<String, Double>();
        assign.put("x", 1d);
        assign.put("y", 2d);
        Polynomial p4 = p1.substitute(assign);
        Assert.assertEquals(p2.toString(), p4.toString());

        assign.clear();
        assign.put("x", 10d);
        p4 = p1.substitute(assign);
        Assert.assertEquals(p3.toString(), p4.toString());

    }

    @Test
    public void testIndefiniteIntegral() throws Exception {
        PolynomialFactory factory = new PolynomialFactory("z", "v", "w", "x", "y");
        Polynomial p1 = factory.makePolynomial("x^(2)*y^(2) + 3*x^(2) + 5*y^(2)");
        p1.replaceThisWithIndefiniteIntegral("x");
        Assert.assertEquals("5.0*x^(1.0)*y^(2.0)+1.0*x^(3.0)+0.3333333333333333*x^(3.0)*y^(2.0)", p1.toString());
    }

    @Test
    public void testEqualityHash() throws Exception {
        PolynomialFactory factory = new PolynomialFactory("x y z".split(" "));
        Polynomial p1 = factory.makePolynomial("1.0*x^(1.0)*z^(0.0) + 3.66*z^(1)");
        Polynomial p2 = factory.makePolynomial("3*z^(1) + 0.66*z^(1.0) + x^(1)");
        Assert.assertEquals(p1, p2);
        Polynomial p3 = factory.makePolynomial("3.66000001*z^(1) + x^(1)");
        Assert.assertFalse(p1.equals(p3));
    }
}
