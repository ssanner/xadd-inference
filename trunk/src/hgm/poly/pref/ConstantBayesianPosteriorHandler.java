package hgm.poly.pref;

import hgm.poly.ConstrainedExpression;
import hgm.poly.Function;
import hgm.poly.Polynomial;
import hgm.poly.PolynomialFactory;
import hgm.sampling.VarAssignment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 10/03/14
 * Time: 5:05 PM
 */

@Deprecated //I think it should totally be replaced by Bayesian posterior handler
public class ConstantBayesianPosteriorHandler implements Function {
    PolynomialFactory factory;
    int maxAllowedGatingViolations;
    double indicatorNoise;

    ConstrainedExpression<Polynomial> prior;

    List<Polynomial> posGatingConstraints = new ArrayList<Polynomial>();
    List<Polynomial> negGatingConstraints = new ArrayList<Polynomial>();
    List<Boolean> reusableGatingMask = new ArrayList<Boolean>();

    public ConstantBayesianPosteriorHandler(PolynomialFactory factory, ConstrainedExpression prior, double indicatorNoise, int maxAllowedGatingViolations) {
        this.factory = factory;
        this.prior = prior;

        this.maxAllowedGatingViolations = maxAllowedGatingViolations;
        this.indicatorNoise = indicatorNoise;
    }

    public void addPositiveConstraint(Polynomial positiveConstraint) {
        posGatingConstraints.add(positiveConstraint);
        Polynomial cloned = positiveConstraint.clone();
        cloned.multiplyScalarInThis(-1); //negative constraint
        negGatingConstraints.add(cloned);

        reusableGatingMask.add(null);
    }

    public PolynomialFactory getPolynomialFactory() {
        return factory;
    }

    @Deprecated
    @Override
    public double evaluate(VarAssignment fullVarAssign) {
        Double[] varValues = factory.getReusableVarValues(fullVarAssign.getContinuousVarAssign());
        return evaluate(varValues);
    }

    @Override
    public String[] collectContinuousVars() {
        return factory.getAllVars();  //todo what if all factory vars are not used
    }

    //evaluate and save the activated gating variables
    public double evaluate(Double[] reusableSample) {
        double priorEval = prior.evaluate(reusableSample);
        if (priorEval < 0) throw new RuntimeException("negative prior!");
        if (priorEval == 0) return 0;

        double c =1d;
        for (int i = 0; i < posGatingConstraints.size(); i++) {
            double evalConstraint = posGatingConstraints.get(i).evaluate(reusableSample);
            if (evalConstraint > 0) {  //what about equality?
                c *= (1 - indicatorNoise);
//                   lastGateActivationMask.set(i, true);
            } else {
                c *= indicatorNoise;
//                lastGateActivationMask.set(i, false);
            }
        }

        return priorEval * c;
    }

//    public List<Boolean> getLastGateActivationMask() {
//        return lastGateActivationMask;
//    }

    //todo: this should return 'void' and 'reusable gating mask' should be accessed independently, to alleviate any possible bug...
    //length of assignment = dimension of the space; length of mask = number of observations...
    public List<Boolean> adjustedReusableGateActivationMask(Double[] assignment) {
        for (int i = 0; i < posGatingConstraints.size(); i++) {
            double evalConstraint = posGatingConstraints.get(i).evaluate(assignment);
            reusableGatingMask.set(i, evalConstraint > 0);  //what about equality?
        }

        return reusableGatingMask;
    }

    //todo by reusing objects this may be faster...
    public ConstrainedExpression makePolytope(List<Boolean> gateMask) {
        List<Polynomial> constraints = new ArrayList<Polynomial>(gateMask.size() + prior.getConstraints().size());
        double c =1d;
        for (int i = 0; i < gateMask.size(); i++) {
            if (gateMask.get(i)) {
                constraints.add(posGatingConstraints.get(i));
                c *= (1 - indicatorNoise);
            } else {
                constraints.add(negGatingConstraints.get(i));
                c *= indicatorNoise;
            }
        }

        Polynomial clonedPrior = prior.getFruit().clone();
        clonedPrior.multiplyScalarInThis(c);

        constraints.addAll(prior.getConstraints());
        return new ConstrainedExpression(clonedPrior, constraints);
    }

    public int numberOfConstraints(){
        return posGatingConstraints.size();
    }
}
