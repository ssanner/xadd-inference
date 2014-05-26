package hgm.poly.bayesian;

import hgm.poly.*;
import hgm.poly.pref.FatalSamplingException;
import hgm.sampling.VarAssignment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 10/03/14
 * Time: 5:05 PM
 * <p/>
 * Generalization of BPPL PosteriorHandler
 */
public class GeneralBayesianPosteriorHandler implements Function {
    public static final boolean DEBUG = true;
    PolynomialFactory factory;

//    PiecewisePolynomial prior;
    PriorHandler priorHandler;

    List<PiecewisePolynomial> likelihoods = new ArrayList<PiecewisePolynomial>();
    private List<Integer> likelihoodsNumCases = new ArrayList<Integer>(); //size of the case statements of each likelihood

    //todo rename
    List<Integer> reusableLikelihoodGatingMask = new ArrayList<Integer>(); //active case-statement of each likelihood

/*
    public GeneralBayesianPosteriorHandler(PiecewisePolynomial prior) {
        this.factory = prior.getFactory();
        this.prior = prior;
    }
*/

    public GeneralBayesianPosteriorHandler(PriorHandler priorHandler) {
        this.factory = priorHandler.getFactory();
        this.priorHandler = priorHandler;
    }

    public void addLikelihood(PiecewisePolynomial likelihood) {
        likelihoods.add(likelihood);
        likelihoodsNumCases.add(likelihood.numCases());
        reusableLikelihoodGatingMask.add(null); //since no sentence is chosen yet...
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
        double priorEval = priorHandler.getPrior().evaluate(reusableSample);
        if (priorEval < 0) throw new RuntimeException("negative prior!");
        if (priorEval == 0) return 0;

        double c = 1d; //product of likelihoods
        for (PiecewisePolynomial likelihood : likelihoods) {
            double likelihoodEval = likelihood.evaluate(reusableSample);
            c *= likelihoodEval;

        }

        return priorEval * c;
    }

//    public List<Boolean> getLastGateActivationMask() {
//        return lastGateActivationMask;
//    }

    //todo: this should return 'void' and 'reusable gating mask' should be accessed independently, to alleviate any possible bug...
    //length of assignment = dimension of the space; length of mask = number of observations...
    public List<Integer> adjustedReusableGateActivationMask(Double[] assignment) throws FatalSamplingException {
        for (int i = 0; i < likelihoods.size(); i++) {
            PiecewisePolynomial likelihood = likelihoods.get(i);
            reusableLikelihoodGatingMask.set(i, likelihood.getActivatedCaseId(assignment));
        }

        return reusableLikelihoodGatingMask;
    }

    public PiecewisePolynomial makeActivatedSubFunction(List<Integer> gateMask) {
        int numLikelihoods = gateMask.size();
        List<Polynomial> productOfActiveLikelihoodSegments = new ArrayList<Polynomial>(numLikelihoods);
        Polynomial productOfActiveLikelihoodsPoly = factory.one();

        for (int i = 0; i < numLikelihoods; i++) {
            PiecewisePolynomial likelihood = likelihoods.get(i);
            int caseId = gateMask.get(i);
            ConstrainedPolynomial activeCase = likelihood.getCases().get(caseId);
            productOfActiveLikelihoodSegments.addAll(activeCase.getConstraints());
            productOfActiveLikelihoodsPoly = productOfActiveLikelihoodsPoly.multiply(activeCase.getPolynomial());
        }

        return priorHandler.getPrior().multiply(productOfActiveLikelihoodSegments, productOfActiveLikelihoodsPoly);

        //
//        List<Polynomial> constraints = new ArrayList<Polynomial>(gateMask.size() + prior.getConstraints().size());
//        constraints.addAll(prior.getConstraints());
//        Polynomial poly = prior.getPolynomial().clone(); //cloning is not needed?
//        for (int i = 0; i < gateMask.size(); i++) {
//            PiecewisePolynomial likelihood = likelihoods.get(i);
//            int caseId = gateMask.get(i);
//            ConstrainedPolynomial activeCase = likelihood.getCases().get(caseId);
//            constraints.addAll(activeCase.getConstraints());
//            poly = poly.multiply(activeCase.getPolynomial());
//        }
//        return new ConstrainedPolynomial(poly, constraints);
    }

    public int numberOfLikelihoods() {
        return likelihoods.size();
    }

    public PriorHandler getPriorHandler() {
        return priorHandler;
    }

    public int getNumCasesInLikelihood(int likelihoodIndex) {
        return likelihoodsNumCases.get(likelihoodIndex);
    }

    public List<Integer> getNumCasesInAllLikelihoods() {
        return likelihoodsNumCases;
    }

    public List<PiecewisePolynomial> getLikelihoods() {
        return likelihoods;
    }
}
