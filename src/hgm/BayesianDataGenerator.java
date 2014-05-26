package hgm;

import hgm.poly.bayesian.PriorHandler;

import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 29/04/14
 * Time: 4:17 PM
 */
public abstract class BayesianDataGenerator<T> {
    protected PriorHandler prior;
    private int numParams;

    protected BayesianDataGenerator(PriorHandler prior) {
        this.prior = prior;
        this.numParams = prior.getFactory().numberOfVars();
    }

    public PriorHandler getPrior() {
        return prior;
    }

    public int getNumberOfParameters() {   // i.e. #attributes in Pref. learning or #instrumentTypes in MM
        return numParams;
    }

    public abstract List<T> getObservedDataPoints();  //i.e. preference responses  or trading responses

}
