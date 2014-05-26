package hgm.poly.market;

import hgm.BayesianDataGenerator;
import hgm.poly.bayesian.PriorHandler;

/**
 * Created by Hadi Afshar. Date: 19/12/13 Time: 8:33 PM
 */
public abstract class MarketMakingDatabase extends BayesianDataGenerator<TradingResponse> {
    double starVarEpsilon; //just a param.

    protected MarketMakingDatabase(PriorHandler prior, double starVarEpsilon) {
        super(prior);
        this.starVarEpsilon = starVarEpsilon;
    }

    public double getStarVarEpsilon() {
        return starVarEpsilon;

    }

}
