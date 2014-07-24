package hgm.poly.integral;

import hgm.poly.ConstrainedPolynomial;
import hgm.poly.Polynomial;
import hgm.poly.PolynomialFactory;
import hgm.poly.vis.FunctionVisualizer;
import org.junit.Test;
import org.testng.Assert;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 2/04/14
 * Time: 4:50 PM
 */
public class SymbolicMultiDimPolynomialIntegralTest {
    public static void main(String[] args) {
        SymbolicMultiDimPolynomialIntegralTest instance = new SymbolicMultiDimPolynomialIntegralTest();
        instance.testIntegral1();
    }

    @Test
    public void testIntegral1() {
        PolynomialFactory f = new PolynomialFactory("x", "y");
//        Polynomial p1 = f.makePolynomial("x^(2)*y^(2) + 3*y^(2)*x^(2) + 4*x^(2) + 1*x^(1)");
        Polynomial p1 = f.makePolynomial("x^(1) + 0.1*y^(1) + 0.05");//("x^(2) + y^(2) + 5");

        List<Polynomial> cnstrns = Arrays.asList(
                f.makePositiveConstraint("1*x^(1)>0"),
                f.makePositiveConstraint("1*x^(1)+-5<0"),
                f.makePositiveConstraint("1*y^(1)>0"),
                f.makePositiveConstraint("1*y^(1)+-5<0"),
                f.makePositiveConstraint("1*x^(1) + 1*y^(1) + -7<0")
        );
        ConstrainedPolynomial cp = new ConstrainedPolynomial(p1, cnstrns);
        FunctionVisualizer.visualize(cp, -4, 8, 0.2, "cp");

        SymbolicMultiDimPolynomialIntegral integral = new SymbolicMultiDimPolynomialIntegral();
        SymbolicOneDimFunctionGenerator genX = integral.integrate(cp, "x");
        OneDimFunction fx = genX.makeFunction(new Double[]{null, 4d});
        double eval = fx.eval(2);
        System.out.println("eval = " + eval);
        FunctionVisualizer.visualize(fx, -10,10,0.1, "fx");
    }

    @Test
    public void testSort() {
        PolynomialFactory f = new PolynomialFactory("x", "y");
        SymbolicMultiDimPolynomialIntegral integral = new SymbolicMultiDimPolynomialIntegral();

        Polynomial[] sorted = integral.sortWithRespectTo(f.makePolynomial("x^(2) + 3*y^(2) + 5"), "x");
        Assert.assertEquals(Arrays.toString(sorted), "[3.0*y^(2.0)+5.0, , 1.0]");

        Assert.assertEquals(Arrays.toString(integral.sortWithRespectTo(f.makePositiveConstraint("1*y^(1)+-5<0"), "y")), "[5.0, -1.0]");
        Assert.assertEquals(Arrays.toString(integral.sortWithRespectTo(f.makePositiveConstraint("2*x^(1) + 3*y^(1) + -7<0"), "y")), "[-2.0*x^(1.0)+7.0, -3.0]");

//        Function in = integral.integrate(cp, "x");
//        FunctionVisualizer.visualize(in, -4, 8, 0.2, "cp");
    }
}
