package hgm.poly.gm;

import hgm.poly.PiecewisePolynomial;

import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 23/07/14
 * Time: 6:20 PM
 */
public class DeterministicFactor extends Factor{
    // Delta(associatedVar - g)  indicating: associated_var = g
    protected DeterministicFactor(PiecewisePolynomial g, String associatedVar) {
        super(g, associatedVar);
    }

    @Override
    public DeterministicFactor substitute(Map<String, Double> assign) {
        PiecewisePolynomial substitutedPP = this.piecewisePolynomial.substitute(assign);
        return new DeterministicFactor(substitutedPP, this.associatedVar);
    }
}
