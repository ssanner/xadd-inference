package hgm.poly.integral.frac;


import hgm.poly.ConstrainedExpression;
import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;
import hgm.poly.PolynomialFactory;
import org.junit.Test;

import java.util.Arrays;
import java.util.Set;

/**
 * Created by Hadi Afshar.
 * Date: 1/08/14
 * Time: 7:47 AM
 */
public class DigesterTest {
    @Test
    public void test1() {
        PolynomialFactory factory = new PolynomialFactory("w x y z".split(" "));

        PiecewiseExpression<Fraction> pf = new PiecewiseExpression<Fraction>(
                new ConstrainedExpression<Fraction>(factory.makeFraction("1"),
                        Arrays.asList(factory.makeFraction("[x^(1)]/[x^(2) + -20*y^(1)]"), factory.makeFraction("[y^(1)+5]/[x^(1)*y^(1)*z^(1)]"))));

        Digester digester = new Digester(pf, "x");
        Set<Digester.IntermediateConstraintExpression> inters = digester.getInters();
        System.out.println("inters.size() = " + inters.size());
        System.out.println("inters = " + inters);

    }

    @Test
    public void test2() {
        PolynomialFactory factory = new PolynomialFactory("w x y z".split(" "));

        PiecewiseExpression<Fraction> pf = new PiecewiseExpression<Fraction>(
                new ConstrainedExpression<Fraction>(factory.makeFraction("1"),
                        Arrays.asList(factory.makeFraction("[666]/[x^(1)*y^(1)*z^(1)]"))));

        Digester digester = new Digester(pf, "x");
        Set<Digester.IntermediateConstraintExpression> inters = digester.getInters();
        System.out.println("inters.size() = " + inters.size());
        System.out.println("inters = " + inters);

    }

}
