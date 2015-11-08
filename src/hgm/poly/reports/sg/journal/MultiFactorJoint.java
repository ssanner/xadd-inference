package hgm.poly.reports.sg.journal;

import hgm.poly.FactorizedPiecewiseStructure;
import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;
import hgm.poly.PolynomialFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created by Hadi M Afshar.
 * Date: 11/3/15
 * Time: 6:26 AM
 */
public class MultiFactorJoint implements FactorizedPiecewiseStructure<Fraction> {
    List<PiecewiseExpression<Fraction>> factors;
    PolynomialFactory factory;
    SortedSet<String> scopeVars;

    public MultiFactorJoint(List<PiecewiseExpression<Fraction>> factors) {
        this.factors = factors;

        factory = factors.get(0).getFactory();

        scopeVars = new TreeSet<String>();
        for (PiecewiseExpression<Fraction> factor : factors) {
            scopeVars.addAll(factor.getScopeVars());
        }
    }

    @Override
    public PolynomialFactory getFactory() {
        return factory;
    }

    @Override
    public double evaluate(Double[] assign) {
        double r = 1.0;
        for (PiecewiseExpression<Fraction> factor : factors) {
            r = r * factor.evaluate(assign);
        }
        return r;
    }

    @Override
    public SortedSet<String> getScopeVars() {
        return scopeVars;
    }

    @Override
    public FactorizedPiecewiseStructure<Fraction> substitute(Double[] assign) {
        List<PiecewiseExpression<Fraction>> newFactors = new ArrayList<PiecewiseExpression<Fraction>>(factors.size());
        for (PiecewiseExpression<Fraction> factor : factors) {
            newFactors.add(factor.substitute(assign));
        }
        return new MultiFactorJoint(newFactors);
    }

    @Override
    public List<PiecewiseExpression<Fraction>> getFactorsNotInvolving(String var) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public List<PiecewiseExpression<Fraction>> getFactorsInvolving(String var) {
        List<PiecewiseExpression<Fraction>> l = new ArrayList<PiecewiseExpression<Fraction>>();
        for (PiecewiseExpression<Fraction> factor : factors) {
            l.addAll(factor.getFactorsInvolving(var));
        }
        return l;
    }

    @Override
    public String toString() {
        int i = 1;
        StringBuilder sb = new StringBuilder();
        for (PiecewiseExpression<Fraction> factor : factors) {
            sb.append("MultiFactor. factor #" + (i++) + ":\n" + factor + "\n");
        }
        return sb.toString();
    }
}
