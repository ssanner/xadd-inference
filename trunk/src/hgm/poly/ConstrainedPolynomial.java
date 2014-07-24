package hgm.poly;

import hgm.sampling.VarAssignment;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 21/02/14
 * Time: 2:26 PM
 */
public class ConstrainedPolynomial implements Function {
    private Polynomial polynomial;
    /**
     * By assumption, all constraints should be > 0
     */
    private CaseStatementConstraints constraints;  // private Set<Polynomial> constraints;

    public ConstrainedPolynomial(Polynomial polynomial, Collection<Polynomial> constraints) {
        this.polynomial = polynomial;
        this.constraints = new CaseStatementConstraints(constraints); //todo making a new set is maybe useless...
    }

    public Polynomial getPolynomial() {
        return polynomial;
    }

    public CaseStatementConstraints getConstraints() {
        return constraints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConstrainedPolynomial that = (ConstrainedPolynomial) o;

        if (!constraints.equals(that.constraints)) return false;
        if (!polynomial.equals(that.polynomial)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = polynomial.hashCode();
        result = 31 * result + constraints.hashCode();
        return result;
    }

    //    public int getConstraintsHashCode() {
//        int h = 0;
//        for (Polynomial constraint : constraints) {
//            h += constraint.hashCode(); //treating the constraints as a set...
//        }
//        return h;
//    }

//    public void setConstraints(List<Polynomial> augmentedConstraints) {
//        this.constraints = augmentedConstraints;
//    }

    //NOTE: although factory is not checked for speed clearly the new value should be associated to the same factory
    public void setPolynomial(Polynomial polynomial) {
        this.polynomial = polynomial;
    }

    public PolynomialFactory getPolynomialFactory() {
        return polynomial.getFactory();
    }


    @Override
    @Deprecated
    public double evaluate(VarAssignment fullVarAssign) {
        Double[] varValues = polynomial.getFactory().getReusableVarValues(fullVarAssign.getContinuousVarAssign());
        return evaluate(varValues);
    }

    public double evaluate(Double[] fullVarAssign) {
        for (Polynomial constraint : constraints) {
            if (constraint.evaluate(fullVarAssign) <= 0.0) return 0; //what about equality?
        }

        return polynomial.evaluate(fullVarAssign);
    }

    public ConstrainedPolynomial substitute(Double[] continuousVarAssign) {
        List<Polynomial> instantiatedConstraints = new ArrayList<Polynomial>(constraints.size());
        for (Polynomial constraint : constraints) {
            instantiatedConstraints.add(constraint.substitute(continuousVarAssign));
        }

        return new ConstrainedPolynomial(polynomial.substitute(continuousVarAssign), instantiatedConstraints);
    }

    @Override
    public String[] collectContinuousVars() {
//        return polynomial.getFactory().getAllVars(); //but active vars should be returned not all vars....
        Set<String> scopeVarsSet = getScopeVars();
        String[] scopeVars = new String[scopeVarsSet.size()];
        int i = 0;
        for (String v : scopeVars) {
           scopeVars[i++] = v;
        }
        return scopeVars;
    }

    public Set<String> getScopeVars(){
        Set<String> scopeVars = new HashSet<String>();
        for (Polynomial constraint : constraints) {
            scopeVars.addAll(constraint.getScopeVars());
        }
        scopeVars.addAll(polynomial.getScopeVars());
        return scopeVars;
    }

    public ConstrainedPolynomial substitute(Map<String, Double> assign) {
        return new ConstrainedPolynomial(this.polynomial.substitute(assign), this.constraints.substitute(assign));
    }


    @Override
    public String toString() {
        return "ConstrainedPolynomial{" +
                polynomial +
                "\t\t\t IF: " + constraints + "  all > 0" +
                '}';
    }

}
