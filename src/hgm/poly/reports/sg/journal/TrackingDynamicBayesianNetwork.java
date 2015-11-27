package hgm.poly.reports.sg.journal;

import hgm.poly.ConstrainedExpression;
import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;
import hgm.poly.PolynomialFactory;
import hgm.poly.gm.*;

import java.util.*;

/**
 * Created by Hadi M Afshar.
 * Date: 11/9/15
 * Time: 4:06 AM
 */
//todo test
public class TrackingDynamicBayesianNetwork extends BayesNetGraphicalModel {
    int n;// numbers of layers:
    String stdDev = "2.0"; //todo check with XADD...

    public TrackingDynamicBayesianNetwork(int n) {
        this.n = n;

        // b_1 ---> b_2 ---> ... ---> b_n
        //   \-->      \-->              \-->
        //        o_1       o_2    ...        o_n
        //   /-->      /-->              /-->
        // x_1 ---> x_2 ---> ... ---> x_n

        String[] vars = new String[3 * n];
        String[] bVars = new String[n]; //binary vars are also in "vars"
        for (int i = 0; i < n; i++) {
            bVars[i] = "b_" + (i + 1);
            vars[3 * i] = "b_" + (i + 1);
            vars[3 * i + 1] = "x_" + (i + 1);
            vars[3 * i + 2] = "o_" + (i + 1);
        }


        PolynomialFactory factory = new PolynomialFactory(vars);
        Distributions dBank = new Distributions(factory);

        PiecewiseExpression<Fraction>[] bGivenPreviousB = new PiecewiseExpression[n];  //pr(b_i | b_{i-1})
        PiecewiseExpression<Fraction>[] xGivenPreviousX = new PiecewiseExpression[n];  //pr(x_i | x_{i-1})
        PiecewiseExpression<Fraction>[] oGivenBX = new PiecewiseExpression[n]; //pr(o_i | b_i, x_i)

        for (int i = 0; i < n; i++) {
            if (i == 0) {
                // pr(b1) =
                // if b1 (>0) then 0.2
                // if -b1 (>0) then 0.8
                ConstrainedExpression<Fraction> bCase1 = new ConstrainedExpression<Fraction>(factory.makeFraction("0.2"), Arrays.asList(factory.makeFraction("b_1^(1)")));
                ConstrainedExpression<Fraction> bCase2 = new ConstrainedExpression<Fraction>(factory.makeFraction("0.8"), Arrays.asList(factory.makeFraction("-1*b_1^(1)")));
                bGivenPreviousB[i] = new PiecewiseExpression<Fraction>(true, bCase1, bCase2);

                // p(x_1):
                xGivenPreviousX[i] = dBank.createUniformDistributionFraction("x_1", "0.0", "10.0");

            } else {
                String bi = "b_" + (i + 1) + "^(1)";
                String bp = "b_" + (i) + "^(1)"; //previous b: b_{i-1}
                // pr(b_i | b_{i-1}) =
                // if -b_{i-1} (>0), -b_{i} (>0) :0.8
                // if -b_{i-1} (>0),  b_{i} (>0) :0.2
                // if  b_{i-1} (>0),  b_{i} (>0) :1.0
                // if  b_{i-1} (>0), -b_{i} (>0) :0.0  (otherwise)
                ConstrainedExpression<Fraction> bCase1 = new ConstrainedExpression<Fraction>(factory.makeFraction("0.8"),
                        Arrays.asList(factory.makeFraction("-1*" + bp), factory.makeFraction("-1*" + bi)));
                ConstrainedExpression<Fraction> bCase2 = new ConstrainedExpression<Fraction>(factory.makeFraction("0.2"),
                        Arrays.asList(factory.makeFraction("-1*" + bp), factory.makeFraction(bi)));
                ConstrainedExpression<Fraction> bCase3 = new ConstrainedExpression<Fraction>(factory.makeFraction("1.0"),
                        Arrays.asList(factory.makeFraction(bp), factory.makeFraction(bi)));
                bGivenPreviousB[i] = new PiecewiseExpression<Fraction>(false, bCase1, bCase2, bCase3);

                String xi = "x_" + (i + 1);
                String xp = "x_" + (i) + "^(1)"; //previous x: x_{i-1}
                //p(x_i | x_{i-1}) = N(x_i; x_{i-1}, sigma):
                xGivenPreviousX[i] = dBank.createNormalDistributionViaIrwinHallApprox(xi, xp, stdDev);
            }

            String oi = "o_" + (i + 1);
            String xi = "x_" + (i + 1) + "^(1)";
            String bi = "b_" + (i + 1) + "^(1)";
            PiecewiseExpression<Fraction> oIfNotB = dBank.createExhaustiveTriangular(oi, xi, "5.0", "5.0");//todo make sure XADD uses same bounds
            oIfNotB.addConstraintsToAllCasesInThis(Arrays.asList(factory.makeFraction("-1*" + bi)), true);
            PiecewiseExpression<Fraction> oIfB = dBank.createExhaustiveTriangular(oi, xi, "5.0", "1.0");//todo make sure XADD uses same bounds
            oIfB.addConstraintsToAllCasesInThis(Arrays.asList(factory.makeFraction(bi)), true);
            //all Cases:
            List<ConstrainedExpression<Fraction>> allCases = oIfB.getCases();
            allCases.addAll(oIfNotB.getCases());
            oGivenBX[i] = new PiecewiseExpression<Fraction>(false, allCases);
        }

        for (int i = 0; i < n; i++) {
            this.addFactor(new StochasticVAFactor("b_" + (i + 1), bGivenPreviousB[i]));
            this.addFactor(new StochasticVAFactor("x_" + (i + 1), xGivenPreviousX[i]));
            this.addFactor(new StochasticVAFactor("o_" + (i + 1), oGivenBX[i]));
        }
    }

    public MultiFactorJoint makeJoint(List<String> queryVars, Map<String, Double> evidence) {
        //step 0.
        Set<String> queryAndEvidenceVars = new HashSet<String>(queryVars.size() + evidence.size());
        queryAndEvidenceVars.addAll(queryVars);
        queryAndEvidenceVars.addAll(evidence.keySet());

        List<Factor> originalFactors = allInferenceRelevantFactors(queryAndEvidenceVars); //in BNs this is the factors of query, evidence and their ancestors.

        //step 1.
        List<PiecewiseExpression<Fraction>> factors = instantiateObservedFactors(originalFactors, evidence);

        return new MultiFactorJoint(factors);

    }

    List<PiecewiseExpression<Fraction>> instantiateObservedFactors(List<Factor> factors, Map<String, Double> evidence) {
        List<PiecewiseExpression<Fraction>> results = new ArrayList<PiecewiseExpression<Fraction>>(factors.size());
        for (Factor factor : factors) {
            Factor subst = factor.substitution(evidence);
            PiecewiseExpression<Fraction> piecewiseFraction = ((StochasticVAFactor) subst).getPiecewiseFraction();
            results.add(piecewiseFraction);
        }
        return results;
    }
}
