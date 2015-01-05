package hgm.poly.gm;

import hgm.poly.*;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 23/07/14
 * Time: 6:20 PM
 */
public class DeterministicFactor extends VarAssociatedFactor {
    /**
     * Equality expression
     */
    private Fraction g; //todo in future it would be a piecewise fraction
    private Set<String> parentVars;

    // Delta(associatedVar - g)  indicating: associated_var = g
    public DeterministicFactor(String associatedVar, Fraction g) {
        super.associatedVar = associatedVar;
        this.g = g;
        parentVars = g.getScopeVars();
        if (parentVars.contains(associatedVar)) throw new RuntimeException("associated var should not be in G");
    }

    @Override
    public Set<String> getParentVars() {
        return parentVars;
    }

    @Override
    public DeterministicFactor substitution(Map<String, Double> assign) {
        Fraction substitutedG = g.substitute(assign);
        return new DeterministicFactor(this.associatedVar, substitutedG);
    }

    @Override
    public DeterministicFactor substitution(String var, Expression value) {
        return new DeterministicFactor(this.associatedVar, g.substitute(var, (Fraction) value));     //todo???
    }

    @Override
    public String toString() {
        return "DeterministicFactor: \n" + associatedVar + " ::= " + g;
    }

    public Fraction getAssignedExpression() {
        return g;
    }

    @Override
    public PolynomialFactory getFactory() {
        return g.getFactory();
    }

    /* @Override
    public Set<String> getScopeVars() {
        Set<String> result = new HashSet<String>(parentVars);
        result.add(associatedVar);
        return result;
    }*/


    public List<DeterministicFactor> solve(Double value) {
        //at the moment only a single solution is computed...
        return Arrays.<DeterministicFactor>asList(linearSolve(value));
    }

    public DeterministicFactor linearSolve(Double value) {
        // g = value
        String y = findSolutionVar();  //a var

        // let g be (a*y + b) / (c*y + d)
        Polynomial[] nPs = g.getNumerator().sortWithRespectTo(y); //e.g. b + a*y
        Polynomial[] dPs = g.getDenominator().sortWithRespectTo(y); //e.g. d + c*y
        if (nPs.length > 2) throw new RuntimeException("unexpected");
        if (dPs.length > 2) throw new RuntimeException("unexpected");
        Polynomial b = nPs[0];
        Polynomial d = dPs[0];

        Polynomial a = nPs.length == 2 ?
                nPs[1]
                :
                new Polynomial(b.getFactory()); //zero

        Polynomial c = dPs.length == 2 ?
                dPs[1]
                :
                new Polynomial(b.getFactory()); //zero

        //by solving g for y:
        // y = (value*d - b)/(a - value*c)
        Polynomial yNumerator = d.scalarMultiplication(value).returnSubtraction(b);
        Polynomial yDenominator = a.returnSubtraction(c.scalarMultiplication(value));
        Fraction gForY = new Fraction(yNumerator, yDenominator);
        return new DeterministicFactor(y, gForY);
    }

    private String findSolutionVar() { //todo maybe this can be done in a cleverer way
        PolynomialFactory factory = g.getFactory();
        for (String parentVar : parentVars) {
            if ((g.getNumerator().degree(factory.getVarIndex(parentVar)) < 2) &&
                    (g.getDenominator().degree(factory.getVarIndex(parentVar)) < 2)) {
                return parentVar;
            }
        }

        throw new RuntimeException("no proper variable found");
    }
}
