package hgm.poly.bayesian;

import hgm.poly.*;
import hgm.sampling.VarAssignment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 10/03/14
 * Time: 5:05 PM
 *
 * Generalization of BPPL PosteriorHandler
 */
public class BayesianPosteriorHandler implements Function {
    public static final boolean DEBUG = true;
    PolynomialFactory factory;

    ConstrainedPolynomial prior;

    List<PiecewisePolynomial> likelihoods = new ArrayList<PiecewisePolynomial>();
    private List<Integer> likelihoodsNumCases = new ArrayList<Integer>(); //size of the case statements of each likelihood

    //todo rename
    List<Integer> reusableLikelihoodGatingMask = new ArrayList<Integer>(); //active case-statement of each likelihood

    public BayesianPosteriorHandler(PolynomialFactory factory, ConstrainedPolynomial prior) {
        this.factory = factory;
        this.prior = prior;
    }

    public void addLikelihood(PiecewisePolynomial likelihood) {
        likelihoods.add(likelihood);
        likelihoodsNumCases.add(likelihood.numCases());
        reusableLikelihoodGatingMask.add(null); //since no sentence is chosen yet...
//        posGatingConstraints.add(positiveConstraint);
//        Polynomial cloned = positiveConstraint.clone();
//        cloned.multiplyScalarInThis(-1); //negative constraint
//        negGatingConstraints.add(cloned);
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

        double c =1d; //product of likelihoods
        for (PiecewisePolynomial likelihood : likelihoods) {
            double likelihoodEval = likelihood.evaluate(reusableSample);
            c*= likelihoodEval;

        }

        return priorEval * c;
    }

//    public List<Boolean> getLastGateActivationMask() {
//        return lastGateActivationMask;
//    }

    //todo: this should return 'void' and 'reusable gating mask' should be accessed independently, to alleviate any possible bug...
    //length of assignment = dimension of the space; length of mask = number of observations...
    public List<Integer> adjustedReusableGateActivationMask(Double[] assignment) {
        for (int i = 0; i < likelihoods.size(); i++) {
            PiecewisePolynomial likelihood = likelihoods.get(i);
            reusableLikelihoodGatingMask.set(i, likelihood.getActivatedCaseId(assignment));
        }

        return reusableLikelihoodGatingMask;
    }

    //todo by reusing objects this may be faster...
    public ConstrainedPolynomial makePolytope(List<Integer> gateMask) {
        List<Polynomial> constraints = new ArrayList<Polynomial>(gateMask.size() + prior.getConstraints().size());
        constraints.addAll(prior.getConstraints());
        Polynomial poly = prior.getPolynomial().clone(); //todo cloning is not needed?

        double c =1d;
        for (int i = 0; i < gateMask.size(); i++) {
            PiecewisePolynomial likelihood = likelihoods.get(i);
            int caseId = gateMask.get(i);
            ConstrainedPolynomial activeCase = likelihood.getCases().get(caseId);
            constraints.addAll(activeCase.getConstraints());
            poly = poly.multiply(activeCase.getPolynomial());
        }
        return new ConstrainedPolynomial(poly, constraints);
    }

    //todo rename to number of likelihoods...
//    @Deprecated
//    public int numberOfConstraints(){
//        return likelihoods.size();
//    }

    public int numberOfLikelihoods() {
        return likelihoods.size();
    }

    public int getNumCasesInLikelihood(int likelihoodIndex) {
        return likelihoodsNumCases.get(likelihoodIndex);
    }

    public List<Integer> getNumCasesInAllLikelihoods() {
        return likelihoodsNumCases;
    }
}
