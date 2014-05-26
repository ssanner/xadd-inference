package hgm.poly.market;

import hgm.poly.PiecewisePolynomial;
import hgm.poly.bayesian.AbstractGeneralBayesianGibbsSampler;
import hgm.poly.bayesian.PriorHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by Hadi Afshar.
 * Date: 29/04/14
 * Time: 10:54 AM
 */
public class DummyMarketMakingDatabase extends MarketMakingDatabase {
    private Random random = new Random();
//    int numTypes; //number of instrument types
//    private Prior priorOnTypeValues;
    List<TradingResponse> responses;

    //instrument types are parameters. Trades are data points:
    public DummyMarketMakingDatabase(
            int numTrades,
            PriorHandler priorOnTypeValues,
            double vStarEpsilon
    ) {
        super(priorOnTypeValues, vStarEpsilon);

        Double[] actualTypeValues = takeSampleFrom(this.prior);

        responses = new ArrayList<TradingResponse>(numTrades);
        for (int t = 0; t < numTrades; t++) {
            int instrumentType = random.nextInt(getNumberOfParameters());
            double a;   //ask and bid prices....
            double b;

            double lb = prior.getLowerBoundsPerDim()[instrumentType];
            double ub = prior.getUpperBoundsPerDim()[instrumentType];

            do {
                a = uniform(lb, ub);
                b = uniform(lb, ub);
            } while (b >= a); //b[i] should always be less than a[i]

            double v = actualTypeValues[instrumentType];

            double vStar_t = uniform(v - vStarEpsilon, v + vStarEpsilon);

            double probBuy = (vStar_t >= a) ? 0.8 : 0.1;
            double probSell = (vStar_t <= b) ? 0.8 : 0.1;


            TradersChoice choice;
            double s = random.nextDouble();
            if (s <= probBuy) {
                choice = TradersChoice.BUY;
            } else {
                s = random.nextDouble();
                if (s <= probSell) {
                    choice = TradersChoice.SELL;
                } else {
                    choice = TradersChoice.NO_DEAL;
                }
            }

//            choice = TradersChoice.NO_DEAL; //hack:

            responses.add(new TradingResponse(instrumentType, b, a, choice));
        }
    }

    private double uniform(double low, double high) {
        return random.nextDouble() * (high - low) + low;
    }

    @Override
    public List<TradingResponse> getObservedDataPoints() {
        return responses;
    }


    private Double[] takeSampleFrom(PriorHandler prior) {
        // By rejection sampling:
        double[] cVarMins = prior.getLowerBoundsPerDim();
        double[] cVarMaxes = prior.getUpperBoundsPerDim();
        double envelope = prior.getFunctionUpperBound();
        PiecewisePolynomial priorPiecewisePolynomial = prior.getPrior();

        Double[] sample = new Double[getNumberOfParameters()];
        for (; ; ) {
            for (int i = 0; i < sample.length; i++) {
                sample[i] = AbstractGeneralBayesianGibbsSampler.randomDoubleUniformBetween(cVarMins[i], cVarMaxes[i]);
            }

            double v = priorPiecewisePolynomial.evaluate(sample);
            double pr = v / envelope;
            if (pr > 1)
                throw new RuntimeException("sampled value: f" + Arrays.toString(sample) + " = " + v + "\t is greater than envelope " + envelope);

            if (pr >= AbstractGeneralBayesianGibbsSampler.randomDoubleUniformBetween(0, 1)) {
                return sample;
            }
        }

    }
}
