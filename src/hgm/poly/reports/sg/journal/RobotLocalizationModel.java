package hgm.poly.reports.sg.journal;

import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;
import hgm.poly.PolynomialFactory;
import hgm.poly.gm.*;

/**
 * Created by Hadi M Afshar.
 * Date: 10/19/15
 * Time: 7:56 PM
 *
 * cvariables (d x_i)
   min-values (-10 -10)
   max-values (20 20)
   bvariables ( )
    d ([U(d,0,0,10)])
     x_i ([0.05*U(x_i,0,0,10) + 0.85*N(x_i,d,2,2.5) + 0.1*T(x_i,10,1,0)])


 ___________________
 NOTES:
 -------------------

 XADD -> Convert2XADD(.)

 N(expr, mu, var, width) where truncated outside +-with
 U(expr, mu, width1, width2) that is from (mu-width1) to (mu+width2) I think
 T(expr, mu, width_l, width_r) that is from (mu-width1) to (mu+width2) peaked at mu I think
 ____________________

 */
public class RobotLocalizationModel extends BayesNetGraphicalModel {
    PolynomialFactory factory;
    Distributions dBank;

    double normalVariance = 2.0;
    Double normalStdDev = Math.sqrt(normalVariance);

    public RobotLocalizationModel(int n /*num data*/) {

        String[] vars = new String[n + 1];
        for (int i = 0; i < n; i++) {
            vars[i] = "x_" + (i);  //resistance, ..
        }
        vars[n] = "d";

        factory = new PolynomialFactory(vars);
        dBank = new Distributions(factory);

//        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();
        this.addFactor(new StochasticVAFactor("d", dBank.createUniformDistributionFraction("d", "0", "10")));

        //PiecewiseExpression<Fraction>[] gsF = new PiecewiseExpression[n];
        for (int i = 0; i < n; i++) {
            String xi = "x_" + (i);
            PiecewiseExpression<Fraction> mix = makeMixture(xi);

            this.addFactor(new StochasticVAFactor(xi, mix));
        }
    }

    PiecewiseExpression<Fraction> makeMixture(String variable) {
        //x_i ([0.05*U(x_i,0,0,10) + 0.85*N(x_i,d,2,2.5) + 0.1*T(x_i,10,1,0)])
        PiecewiseExpression<Fraction> uniform = dBank.createExhaustiveUniformDistributionFraction(variable, "0", "10");
        uniform.multiplyScalarInThis(0.05);

        PiecewiseExpression<Fraction> normal = dBank.createNormalDistributionViaIrwinHallApprox(variable, "d^(1)", normalStdDev.toString());
        normal.multiplyScalarInThis(0.85);

        PiecewiseExpression<Fraction> triangular = dBank.createExhaustiveTriangular(variable, "10", "1", "0");
        triangular.multiplyScalarInThis(0.1);

        PiecewiseExpression<Fraction> total = triangular.add(normal).add(uniform);//triangular;//uniform.add(normal).add(triangular);
        total.makeNonExclusive();

        return total;
    }

}

