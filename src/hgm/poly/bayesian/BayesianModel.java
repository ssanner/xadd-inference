package hgm.poly.bayesian;

import hgm.BayesianDataGenerator;
import hgm.poly.PiecewisePolynomial;
import hgm.poly.PolynomialFactory;

/**
 * Created by Hadi Afshar.
 * Date: 5/05/14
 * Time: 12:45 PM
 */

/**
 * A typical bayesian model has a database and is fed to a sampler...
 */
public abstract class BayesianModel<R> {
    protected BayesianDataGenerator<R> db;
    protected String[] vars;
    protected PolynomialFactory factory;
    protected PriorHandler prior;

    public BayesianModel(BayesianDataGenerator<R> db) {
        this.db = db;
        this.prior = db.getPrior();
        vars = prior.getFactory().getAllVars();
        this.factory = prior.getFactory();
    }

    public PolynomialFactory getFactory() {
        return factory;
    }


    //Pr(parameter theta | observed data R_1 to R_n)
    public GeneralBayesianPosteriorHandler computeBayesianPosterior() {

        GeneralBayesianPosteriorHandler posterior = new GeneralBayesianPosteriorHandler(prior);

        for (R response : db.getObservedDataPoints()) {
            // Pr(q_{ab} | theta):
            PiecewisePolynomial likelihood = computeLikelihoodGivenValueVector(response);
            posterior.addLikelihood(likelihood);
        }

        return posterior;
    }

    protected abstract PiecewisePolynomial computeLikelihoodGivenValueVector(R response);
}
