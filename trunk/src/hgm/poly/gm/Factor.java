package hgm.poly.gm;

import hgm.poly.PiecewisePolynomial;

import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 3/07/14
 * Time: 4:07 AM
 */
public abstract class Factor {
    PiecewisePolynomial piecewisePolynomial;
    String associatedVar;

    protected Factor(PiecewisePolynomial piecewisePolynomial, String associatedVar) {
        this.piecewisePolynomial = piecewisePolynomial;
        this.associatedVar = associatedVar;
    }

    PiecewisePolynomial getPiecewisePolynomial() {
        return piecewisePolynomial;
    }

    public abstract  Factor substitute(Map<String, Double> assign);

    public String getAssociatedVar() {
        return associatedVar;
    }
}
