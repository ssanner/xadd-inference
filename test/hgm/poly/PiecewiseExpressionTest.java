package hgm.poly;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 6/07/14
 * Time: 7:13 AM
 */
public class PiecewiseExpressionTest {
    @Test
    public void testMultiply() throws Exception {
        /*
         * this: e.g.
         *      A1.A2:  f1
         *      A1.A3:  f2
         *      B1:     f3
         *      C:      f4
         * @param other e.g.
         *              A1:     g1
         *              B1.B2:  g2
         *              D:      g3
         * @return
         *      A1.A2:  f1.g1
         *      A1.A3:  f2.g1
         *      B1.A1:  f3.g1
         *      B1.B2:  f3:g2
         *      B1.D:   f3.g3
         *      C.A1:   f4.g1
         *      C.D:    f4.g3
         *
         *     i.e. 7 cases instead of 12 ones.
         */
        PolynomialFactory factory = new PolynomialFactory("x y z f g".split(" "));
        Polynomial a1 = factory.makePolynomial("1.0*x^(1.0) + 1");
        Polynomial a2 = factory.makePolynomial("2.0*x^(2.0)");
        Polynomial a3 = factory.makePolynomial("3.0*y^(2.0)");
        Polynomial b1 = factory.makePolynomial("6.0*y^(2.0)");
        Polynomial c = factory.makePolynomial("30.0*y^(3.0)");
        Polynomial a1_copy = factory.makePolynomial("1*x^(1) + 1"); //to test the effect of equals()
        Polynomial b2 = factory.makePolynomial("1*x^(1) + 1*z^(1)");
        Polynomial d = factory.makePolynomial("1*x^(1) + 666.0*y^(2.0)");  //666 -> 6

        Polynomial f1 = factory.makePolynomial("1*f^(1)");
        Polynomial f2 = factory.makePolynomial("1*f^(2)");
        Polynomial f3 = factory.makePolynomial("1*f^(3)");
        Polynomial f4 = factory.makePolynomial("1*f^(4)");
        Polynomial g1 = factory.makePolynomial("1*g^(1)");
        Polynomial g2 = factory.makePolynomial("1*g^(2)");
        Polynomial g3 = factory.makePolynomial("1*g^(3)");

        PiecewiseExpression pp1 = new PiecewiseExpression( false,
                new ConstrainedExpression(f1, Arrays.asList(a1, a2)),
                new ConstrainedExpression(f2, Arrays.asList(a1, a3)),
                new ConstrainedExpression(f3, Arrays.asList(b1)),
                new ConstrainedExpression(f4, Arrays.asList(c))
        );

        PiecewiseExpression pp2 = new PiecewiseExpression(
                new ConstrainedExpression(g1, Arrays.asList(a1_copy)),
                new ConstrainedExpression(g2, Arrays.asList(b1, b2)),
                new ConstrainedExpression(g3, Arrays.asList(d))
        );

        PiecewiseExpression pp3 = pp1.multiply(pp2);
        Assert.assertTrue(pp3.numCases() == 7);

        /*
                *      A1.A2:  f1.g1
                *      A1.A3:  f2.g1
                *      B1.A1:  f3.g1
                *      B1.B2:  f3:g2
                *      B1.D:   f3.g3
                *      C.A1:   f4.g1
                *      C.D:    f4.g3
         */
        PiecewiseExpression expectedPp3 = new PiecewiseExpression( false,
                new ConstrainedExpression(f1.returnMultiplication(g1), Arrays.asList(a1, a2)),
                new ConstrainedExpression(f2.returnMultiplication(g1), Arrays.asList(a1, a3)),
                new ConstrainedExpression(f3.returnMultiplication(g1), Arrays.asList(b1, a1)),
                new ConstrainedExpression(f3.returnMultiplication(g2), Arrays.asList(b1, b2)),
                new ConstrainedExpression(f3.returnMultiplication(g3), Arrays.asList(b1, d)),
                new ConstrainedExpression(f4.returnMultiplication(g1), Arrays.asList(c, a1)),
                new ConstrainedExpression(f4.returnMultiplication(g3), Arrays.asList(c, d))
                );

        System.out.println("pp3 = " + pp3);
        System.out.println("pp3.hashCode() = " + pp3.hashCode());
        System.out.println("expectedPp3.hashCode() = " + expectedPp3.hashCode());

        System.out.println("expectedPp3 = " + expectedPp3);

        Assert.assertTrue(pp3.equals(expectedPp3));
    }

    @Test
    public void testSub2SuperSet(){
        /*
         * this: e.g.
         *      A1.A2:  f1
         *      A1.A3:  f2
         *      B1:     f3
         *      C:      f4
         * @param other e.g.
         *              A1:     g1
         *              B1.B2:  g2
         *              D:      g3
         * @return
         *      A1.A2:  f1.g1
         *      A1.A3:  f2.g1
         *      B1.A1:  f3.g1
         *      B1.B2:  f3:g2
         *      B1.D:   f3.g3
         *      C.A1:   f4.g1
         *      C.D:    f4.g3
         *
         *     i.e. 7 cases instead of 12 ones.
         */
        PolynomialFactory factory = new PolynomialFactory("x y z f g".split(" "));
        Polynomial a1 = factory.makePolynomial("1.0*x^(1.0) + 1");
        Polynomial a2 = factory.makePolynomial("2.0*x^(2.0)");
        Polynomial a3 = factory.makePolynomial("3.0*y^(2.0)");
        Polynomial b1 = factory.makePolynomial("6.0*y^(2.0)");
        Polynomial c = factory.makePolynomial("30.0*y^(3.0)");
        Polynomial a1_copy = factory.makePolynomial("1.0 + 1*x^(1)"); //to test the effect of equals()
        Polynomial b2 = factory.makePolynomial("1*x^(1) + 1*z^(1)");
        Polynomial d = factory.makePolynomial("1*x^(1) + 666.0*y^(2.0)");  //666 -> 6

        Polynomial f1 = factory.makePolynomial("1*f^(1)");
        Polynomial f2 = factory.makePolynomial("1*f^(2)");
        Polynomial f3 = factory.makePolynomial("1*f^(3)");
        Polynomial f4 = factory.makePolynomial("1*f^(4)");
        Polynomial g1 = factory.makePolynomial("1*g^(1)");
        Polynomial g2 = factory.makePolynomial("1*g^(2)");
        Polynomial g3 = factory.makePolynomial("1*g^(3)");

        PiecewiseExpression pp1 = new PiecewiseExpression(
                new ConstrainedExpression(f1, Arrays.asList(a1, a2)),
                new ConstrainedExpression(f2, Arrays.asList(a1, a3)),
                new ConstrainedExpression(f3, Arrays.asList(b1)),
                new ConstrainedExpression(f4, Arrays.asList(c))
        );

        PiecewiseExpression pp2 = new PiecewiseExpression(
                new ConstrainedExpression(g1, Arrays.asList(a1_copy)),
                new ConstrainedExpression(g2, Arrays.asList(b1, b2)),
                new ConstrainedExpression(g3, Arrays.asList(d))
        );

        Assert.assertEquals(a1, a1_copy);

        Map<CaseStatementConstraints,CaseStatementConstraints> sub2Super = pp2.multCrossProdOp.computeSub2SuperSetMap(pp2, pp1);
        Assert.assertEquals(3, sub2Super.size());
        CaseStatementConstraints a1a2Sup = sub2Super.get(new CaseStatementConstraints(Arrays.asList(a1, a2)));
        Assert.assertEquals(a1a2Sup, new CaseStatementConstraints(Arrays.asList(a1)));
    }
}
