package hgm.poly.gm;

import hgm.poly.Expression;
import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;
import hgm.poly.PolynomialFactory;

import java.util.Map;
import java.util.Set;

/**
 * Created by Hadi Afshar.
 * Date: 25/07/14
 * Time: 5:30 AM
 */
public class StochasticVAFactor extends VarAssociatedFactor {
    private PiecewiseExpression<Fraction> piecewiseFraction;
    private Set<String> parentVars;

    public StochasticVAFactor(String associatedVar, PiecewiseExpression<Fraction> piecewiseFraction) {
        this.piecewiseFraction = piecewiseFraction;
        this.associatedVar = associatedVar;
        parentVars = piecewiseFraction.getScopeVars();
        parentVars.remove(associatedVar);
    }

    @Override
    public Set<String> getParentVars() {
        return parentVars;
    }

    @Override
    public Factor substitution(Map<String, Double> assign) {
        return new StochasticVAFactor(associatedVar, piecewiseFraction.substitute(assign));
    }

    @Override
    public Factor substitution(String var, Expression value) {
        return new StochasticVAFactor(associatedVar, piecewiseFraction.substitute(var, value));  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String toString() {
        return "StochasticVAFactor: \n" + associatedVar + " ~ " + piecewiseFraction;
    }

    public PiecewiseExpression<Fraction> getPiecewiseFraction() {
        return piecewiseFraction;
    }

    @Override
    public PolynomialFactory getFactory() {
        return piecewiseFraction.getFactory();
    }

    /* @Override
    public Set<String> getScopeVars() {
        return piecewiseFraction.getScopeVars();
    }*/
}
