package hgm.poly.market;

import hgm.poly.bayesian.PriorHandler;
import org.junit.Test;

import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 29/04/14
 * Time: 12:25 PM
 */
public class DummyMarketMakingDatabaseTest {
    public static void main(String[] args) {
        DummyMarketMakingDatabaseTest inst = new DummyMarketMakingDatabaseTest();
        inst.test1();
    }

    @Test
    public void test1() {
       MarketMakingDatabase db = new DummyMarketMakingDatabase(5, PriorHandler.uniformInHypercube("v", 5, 1), 2);//(5, 20, 10, 1);
        List<TradingResponse> tradingResponses = db.getObservedDataPoints();
        for (int i = 0; i < tradingResponses.size(); i++) {
            TradingResponse tradingResponse = tradingResponses.get(i);
            System.out.println("[" + i + "]\t\t" +
                    "A: " + tradingResponse.getAskPrice() + "\tB: " + tradingResponse.getBidPrice() +
                    "\t" + tradingResponse.getTradersResponse() + "\tCommodity Id: " + tradingResponse.getCommodityTypeId());
        }
    }
}
