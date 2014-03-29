package hgm.poly;

import hgm.sampling.VarAssignment;

import java.util.ArrayList;
import java.util.List;

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
    private List<Polynomial> constraints;

    public ConstrainedPolynomial(Polynomial polynomial, List<Polynomial> constraints) {
        this.polynomial = polynomial;
        this.constraints = constraints;
    }

    public Polynomial getPolynomial() {
        return polynomial;
    }

    public List<Polynomial> getConstraints() {
        return constraints;
    }

    public PolynomialFactory getPolynomialFactory() {
        return polynomial.getFactory();
    }


    @Override
    @Deprecated
    public double evaluate(VarAssignment fullVarAssign) {
        Double[] varValues = polynomial.getFactory().getReusableVarValues(fullVarAssign.getContinuousVarAssign());
        return evaluate(varValues);
//        for (Polynomial constraint : constraints) {
//            if (constraint.evaluate(fullVarAssign) < 0.0) return 0; //what about equality?
//        }
//        return polynomial.evaluate(fullVarAssign);
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

//    public List<String>[] getActivatedVars(){
//        return acitveVars;
//    }


    //todo: but active vars should be returned not all vars....
    @Override
    public String[] collectContinuousVars() {
        return polynomial.getFactory().getAllVars();
    }


    //todo do I need this?
//    @Override
//    public String[] collectBinaryVars() {
//        return new String[0];
//    }


    @Override
    public String toString() {
        return "ConstrainedPolynomial{" +
                "polynomial=" + polynomial +
                ", constraints=" + constraints +
                '}';
    }
}
