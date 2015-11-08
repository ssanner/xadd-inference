package hgm.poly;

import hgm.sampling.VarAssignment;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 21/02/14
 * Time: 2:26 PM
 */
public class ConstrainedExpression<E extends Expression> implements Function {
    private E fruit;
    /**
     * By assumption, all constraints should be > 0
     */
    private CaseStatementConstraints<E> constraints;  // private Set<Polynomial> constraints;

    public ConstrainedExpression(E fruit, Collection<E> constraints) {
        this.fruit = fruit;
        this.constraints = new CaseStatementConstraints<E>(constraints); //todo making a new set is maybe useless...
    }

    public E getFruit() {
        return fruit;
    }


//NOTE: although factory is not checked for speed clearly the new value should be associated to the same factory
    public void setFruit(E fruit) {
        this.fruit = fruit;
    }

    public CaseStatementConstraints<E> getConstraints() {
        return constraints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConstrainedExpression that = (ConstrainedExpression) o;

        if (!constraints.equals(that.constraints)) return false;
        if (!fruit.equals(that.fruit)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = fruit.hashCode();
        result = 31 * result + constraints.hashCode();
        return result;
    }

//    }

//    }

    public PolynomialFactory getPolynomialFactory() {
        return fruit.getFactory();
    }


    @Override
    @Deprecated
    public double evaluate(VarAssignment fullVarAssign) {
        Double[] varValues = fruit.getFactory().getReusableVarValues(fullVarAssign.getContinuousVarAssign());
        return evaluate(varValues);
    }

    public double evaluate(Double[] fullVarAssign) {
        for (Expression constraint : constraints) {
            if (constraint.evaluate(fullVarAssign) <= 0.0) return 0; //what about equality?
        }

        return fruit.evaluate(fullVarAssign);
    }

    public ConstrainedExpression substitute(Double[] continuousVarAssign) {
        List<Expression> instantiatedConstraints = new ArrayList<Expression>(constraints.size());
        for (Expression constraint : constraints) {
            instantiatedConstraints.add(constraint.substitute(continuousVarAssign));
        }

        return new ConstrainedExpression(fruit.substitute(continuousVarAssign), instantiatedConstraints);
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
        for (Expression constraint : constraints) {
            scopeVars.addAll(constraint.getScopeVars());
        }
        scopeVars.addAll(fruit.getScopeVars());
        return scopeVars;
    }

    public ConstrainedExpression substitute(Map<String, Double> assign) {
        return new ConstrainedExpression(this.fruit.substitute(assign), this.constraints.substitute(assign));
    }

    public ConstrainedExpression substitute(String var, Expression value) {
        return new ConstrainedExpression(this.fruit.substitute(var, value), this.constraints.substitute(var, value));
    }


    @Override
    public String toString() {
        return "ConstrainedPolynomial{" +
                fruit +
                "\t\t\t IF: " + constraints + "  all > 0" +
                '}';
    }

    public void multiplyScalarInThis(double c) {
        this.fruit.multiplyScalarInThis(c);
    }
}
