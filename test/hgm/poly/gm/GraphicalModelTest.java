package hgm.poly.gm;


import hgm.poly.PiecewisePolynomial;
import hgm.poly.Polynomial;
import hgm.poly.PolynomialFactory;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 7/07/14
 * Time: 7:54 AM
 */
public class GraphicalModelTest {
    @Test
    public void test1(){
//        i1 i2 i3
//          \|/
//           z
//          / \
//         o1  o2


        PolynomialFactory factory = new PolynomialFactory("i1 i2 i3 o1 o2".split(" "));
        Distributions dBank = new Distributions(factory);
        PiecewisePolynomial i1F = dBank.createUniformDistribution("i1", "-2", "2");
        System.out.println("i1F = " + i1F);
        PiecewisePolynomial i2F = dBank.createUniformDistribution("i2", "-3", "3");
//        PiecewisePolynomial i3F = dBank.createUniformDistribution("i3", "-2", "2");
        Polynomial z = factory.makePolynomial("1*i1^(1) * 1*i2^(1)");//i1F.add(i2F);
        Polynomial o1LB = z.subtract(factory.makePolynomial("5"));  //z - 5;
        Polynomial o1RB = z.add(factory.makePolynomial("5"));       //z + 5;
        System.out.println("o1LB = " + o1LB);
        PiecewisePolynomial o1F = dBank.createUniformDistribution("o1", o1LB, o1RB);
        System.out.println("o1F = " + o1F);
        PiecewisePolynomial joint = i1F.multiply(i2F).multiply(o1F);
        System.out.println("joint = " + joint);

//        PiecewisePolynomial iF = Distributions.
    }

    @Test
    public void test2() {
        PolynomialFactory factory = new PolynomialFactory("a b c d e f g h i".split(" "));
        Distributions dBank = new Distributions(factory);

        BayesianNetworkGraphicalModel bn = new BayesianNetworkGraphicalModel();

        PiecewisePolynomial ap = dBank.createUniformDistribution("a", "-2", "2");
//        bn.addFactor(new StochasticFactor());
//        bn.addFactor(new );
    }

    //todo simplification of expressions...
    @Test
    public void test3() {
        PolynomialFactory factory = new PolynomialFactory("a b c d e f g h i".split(" "));
        Distributions dBank = new Distributions(factory);
        PiecewisePolynomial f = dBank.createUniformDistribution("a", "-1", "5");
        System.out.println("f = " + f);
        Map<String, Double> assign = new HashMap<String, Double>();
        assign.put("a", 1d);
        PiecewisePolynomial f2 = f.substitute(assign);
        System.out.println("f = " + f);
        System.out.println("f2 = " + f2);


    }
}
