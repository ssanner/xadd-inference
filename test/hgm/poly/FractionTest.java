package hgm.poly;

import hgm.poly.bayesian.AbstractGeneralBayesianGibbsSampler;
import hgm.poly.gm.Distributions;
import hgm.sampling.VarAssignment;
import org.junit.Test;
import org.testng.Assert;

import java.util.Random;

/**
 * Created by Hadi Afshar.
 * Date: 26/07/14
 * Time: 7:07 AM
 */
public class FractionTest {
    @Test
    public void testEquals() throws Exception {
        PolynomialFactory factory = new PolynomialFactory("v", "w", "x", "y");
        Fraction f1 = factory.makeFraction("x^(2)*y^(2) + 3*x^(2) + 5*y^(2)", "x^(1) + y^(1)");
        System.out.println("f1 = " + f1);
        Fraction f2 = factory.makeFraction("3*x^(2) + y^(2)*x^(2) + 5*y^(2)", "y^(1) + x^(1)");
        Assert.assertEquals(f1, f2);

    }

    //todo fraction simplification at least in an elementary level....
    @Test
    public void testAdd() throws Exception {
        PolynomialFactory factory = new PolynomialFactory("a", "b", "c", "d", "e", "f", "x", "y");
        Fraction f1 = factory.makeFraction("a^(2)*b^(1) + 3*c^(1)", "x^(1) + -1*y^(1)");
        System.out.println("f1 = " + f1);
        Fraction f2 = factory.makeFraction("3*x^(2) + y^(2)*x^(2) + 5*y^(2)", "y^(1) + -1*x^(1)");
        System.out.println("f2 = " + f2);
        System.out.println("f1.add(f2) = " + f1.returnAddition(f2));

    }

    @Test
    public void testZero() throws Exception {
        PolynomialFactory factory = new PolynomialFactory("a", "b", "c", "d", "e", "f", "x", "y");

        Fraction f1 = factory.makeFraction("0 * a^(2)*b^(1) + 0 * 3*c^(1)", "x^(1) + 1*x^(1)");
        System.out.println("f1 = " + f1);

//        Fraction f1 = factory.makeFraction("a^(2)*b^(1) + 3*c^(1)", "x^(1) + -1*y^(1)");
//        System.out.println("f1 = " + f1);
//        Fraction f2 = factory.makeFraction("3*x^(2) + y^(2)*x^(2) + 5*y^(2)", "y^(1) + -1*x^(1)");
//        System.out.println("f2 = " + f2);
//        System.out.println("f1.add(f2) = " + f1.addition(f2));

    }

    @Test
    public void testMultiplyScalarInThis() throws Exception {

    }

    @Test
    public void testMultiply() throws Exception {

    }

    @Test
    public void testDivide() throws Exception {

    }

    //todo this test is not appropriate for here...
    @Test
    public void testSubstitute() throws Exception {

        PolynomialFactory factory = new PolynomialFactory("x y z a b c d e f g h i".split(" "));
        Distributions dBank = new Distributions(factory);

//        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();
        PiecewiseExpression xp = dBank.createUniformDistributionFraction("x", "1* a^(1)", "1 * b^(1)");
        System.out.println("xp = " + xp);

        //numerical substitution:
        PiecewiseExpression op = xp.substitute(VarAssignment.str2continuousAssign("a = 1, b = 99"));
        System.out.println("op = " + op);
    }

    @Test
    public void testSymbolicSubstitute() {

        PolynomialFactory factory = new PolynomialFactory("x y z a b c d e f g h i".split(" "));

        //symbolic 'fraction' substitution:
        Fraction f = factory.makeFraction("[5*x^(2)*y^(1) + 6*x^(1)*y^(1)]/[x^(2)+1]");//("[ 1 ] / [c^(1) + d^(1)] ");
        Fraction g = factory.makeFraction("[a^(1)]/[b^(1)]");//("[ 1 ] / [c^(1) + d^(1)] ");
        System.out.println("f = " + f);
        Fraction f2 = f.substitute("x", g);
        Assert.assertEquals("[6.0*a^(1.0)*b^(1.0)*y^(1.0)+5.0*a^(2.0)*y^(1.0)]/[1.0*b^(2.0)+1.0*a^(2.0)]", f2.toString());
        System.out.println("f2 = " + f2);

//            PiecewisePolynomial xp.substitute()
//        PiecewisePolynomial bp = dBank.createUniformDistribution("b", "-2*a^(1)", "2*a^(1)");
//        PiecewisePolynomial cp = new PiecewisePolynomial(new ConstrainedPolynomial(factory.makePolynomial("1*a^(1) + 1*b^(1)"), new HashSet<Polynomial>()));
//        bn.addFactor(new StochasticVAFactor("x", xp));
//        bn.addFactor(new StochasticVAFactor("b", bp));
//        bn.addFactor(new DeterministicFactor("c", cp));


    }

    @Test
    public void testEvaluate() throws Exception {

    }

    @Test
    public void testSubtract() throws Exception {

    }

    @Test
    public void testGetScopeVars() throws Exception {
        PolynomialFactory factory = new PolynomialFactory("a", "b", "c", "d", "e", "f", "x", "y");
        Fraction f1 = factory.makeFraction("a^(2)*b^(1) + 3*c^(1)", "x^(1) + -1*y^(1)");
        System.out.println("f1.getScopeVars() = " + f1.getScopeVars());
        Assert.assertEquals(f1.getScopeVars().size(), 5);
        Assert.assertFalse(f1.getScopeVars().contains("d"));

    }

    @Test
    public void testDerivative() {

        PolynomialFactory factory = new PolynomialFactory("x y z a b c d e f g h i".split(" "));

        Fraction f = factory.makeFraction("[5*x^(2)*y^(1) + 6*x^(1)*y^(1)]/[x^(2)]");
        System.out.println("f = " + f);
        Fraction f2 = f.derivativeWrt("x");
        Assert.assertEquals("[-6.0*x^(2.0)*y^(1.0)]/[1.0*x^(4.0)]", f2.toString());
        System.out.println("f2 = " + f2);

    }

    @Test
    public void absoluteValue() {
        PolynomialFactory factory = new PolynomialFactory("a b c d".split(" "));
        Fraction f = factory.makeFraction("[a^(3.1) + b^(1)]/[c^(1) + d^(1)]");
        System.out.println("f = " + f);
        PiecewiseExpression<Fraction> absF = f.absoluteValue();
        System.out.println("absF = " + absF);

        for (int i=0; i<100000; i++) {
            Double[] assign = {
                    AbstractGeneralBayesianGibbsSampler.randomDoubleUniformBetween(-1, 1),
                    AbstractGeneralBayesianGibbsSampler.randomDoubleUniformBetween(-1, 1),
                    AbstractGeneralBayesianGibbsSampler.randomDoubleUniformBetween(-1, 1),
                    AbstractGeneralBayesianGibbsSampler.randomDoubleUniformBetween(-1, 1)};
            Assert.assertEquals(Math.abs(f.evaluate(assign)), absF.evaluate(assign));
        }

        //....

        Fraction f2 = factory.makeFraction("[3]/[4]");
//        System.out.println("\nf2 = " + f2);
        PiecewiseExpression<Fraction> absF2 = f2.absoluteValue();
        Assert.assertEquals("ConstrainedPolynomial{[3.0]/[4.0]\t\t\t IF: []  all > 0}", absF2.toString().trim());
//        System.out.println("absF2 = " + absF2);

        Fraction f3 = factory.makeFraction("[-3]/[4]");
        PiecewiseExpression<Fraction> absF3 = f3.absoluteValue();
        Assert.assertEquals("ConstrainedPolynomial{[3.0]/[4.0]\t\t\t IF: []  all > 0}", absF3.toString().trim());

        Fraction f4 = factory.makeFraction("[3]/[-4]");
        PiecewiseExpression<Fraction> absF4 = f4.absoluteValue();
        Assert.assertEquals("ConstrainedPolynomial{[-3.0]/[-4.0]\t\t\t IF: []  all > 0}", absF4.toString().trim());

        Fraction f5 = factory.makeFraction("[-3]/[-4]");
        PiecewiseExpression<Fraction> absF5 = f5.absoluteValue();
        Assert.assertEquals("ConstrainedPolynomial{[-3.0]/[-4.0]\t\t\t IF: []  all > 0}", absF5.toString().trim());

        Fraction f6 = factory.makeFraction("[-3*a^(2.0)]/[-4]");
        PiecewiseExpression<Fraction> absF6 = f6.absoluteValue();
//        System.out.println("absF6 = " + absF6);
        Assert.assertEquals("ConstrainedPolynomial{[-3.0*a^(2.0)]/[-4.0]\t\t\t IF: []  all > 0}", absF6.toString().trim());

        Fraction f7 = factory.makeFraction("[-3*a^(2.0)]/[-4*b^(1)]");
        PiecewiseExpression<Fraction> absF7 = f7.absoluteValue();
//        System.out.println("absF7 = " + absF7);
        Assert.assertEquals("ConstrainedPolynomial{[3.0*a^(2.0)]/[-4.0*b^(1.0)]\t\t\t IF: [[-4.0*b^(1.0)]/[1.0]]  all > 0}\n" +
                "\tConstrainedPolynomial{[-3.0*a^(2.0)]/[-4.0*b^(1.0)]\t\t\t IF: [[4.0*b^(1.0)]/[1.0]]  all > 0}", absF7.toString().trim());

        Fraction f8 = factory.makeFraction("[6*c^(1)]/[-3*a^(2.0)]");
        PiecewiseExpression<Fraction> absF8 = f8.absoluteValue();
//        System.out.println("absF8 = " + absF8);
        Assert.assertEquals("ConstrainedPolynomial{[-6.0*c^(1.0)]/[-3.0*a^(2.0)]\t\t\t IF: [[6.0*c^(1.0)]/[1.0]]  all > 0}\n" +
                "\tConstrainedPolynomial{[6.0*c^(1.0)]/[-3.0*a^(2.0)]\t\t\t IF: [[-6.0*c^(1.0)]/[1.0]]  all > 0}", absF8.toString().trim());
    }
}
