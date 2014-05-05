package hgm.poly.market;

import hgm.ModelDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Hadi Afshar.
 * Date: 29/04/14
 * Time: 10:54 AM
 */
public class DummyMarketMakingDatabase implements MarketMakingDatabase {
    private Random random = new Random();
    int numTypes;
    
    @Deprecated
    double vC; //deprecated since instead, joint prior over the distribution of values v_i should be given and they should ba samples from that.

//    int[] tradedTypes; //type of the object traded at time i in [0, numTrades]
//    double[] a;     //value by which trader i can buy object of type 'stepType'
//    double[] b;     //value by which trader i can sell object of type 'stepType'
//    double[] vStar; //value estimated by trader i of the traded object (of type 'stepType')

    List<TradingResponse> responses;

    
    public DummyMarketMakingDatabase(int numInstrumentTypes, int numTrades, double jointPriorOfTypeValues, double epsilon) {
        this.numTypes = numInstrumentTypes;
        this.vC = jointPriorOfTypeValues;

        double[] realTypeValues = new double[numInstrumentTypes];
        for (int i = 0; i < realTypeValues.length; i++) {
            realTypeValues[i] = uniform(-vC, vC); //todo should sample from their joint distribution.
        }

        responses = new ArrayList<TradingResponse>(numTrades);
//        tradedTypes = new int[numTrades];
//        a = new double[numTrades];
//        b = new double[numTrades];
        for (int t = 0; t < numTrades; t++) {
            int instrumentType = random.nextInt(numTypes);
            double a;
            double b;

            do {
                a = uniform(-vC, vC);    //should I use the same bounds for estimating a and b???? should they be affected by the type traded in step t?
                b = uniform(-vC, vC);
            } while (b >= a); //b[i] should always be less than a[i]

            double v = realTypeValues[instrumentType];

            double vStar_t = uniform(v-epsilon, v+epsilon);
//            System.out.println("vStar_t = " + vStar_t);

            double probBuy = (vStar_t >= a) ? 0.8 : 0.1;
            double probSell = (vStar_t <= b) ? 0.8 : 0.1;


            TradersChoice choice;
            double s = random.nextDouble();
            if (s<= probBuy) {
                choice = TradersChoice.BUY;
            } else {
                s = random.nextDouble();
                if (s<= probSell) {
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
    public int getInstrumentTypeCount() {
        return numTypes;
    }

    @Override
    public List<TradingResponse> getTradingResponses() {
        return responses;
    }
}
