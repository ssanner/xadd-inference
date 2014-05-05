package hgm.poly.market;

/**
 * Created by Hadi Afshar.
 * Date: 19/12/13
 * Time: 11:29 PM
 */
public class TradingResponse {
    private int commodityType; // in [0, numCommodityTypes)
    private double bidPrice;  //MM buys in this price (Trader can sell)
    private double askPrice;  //MM sells in this price (Trader can buy)
    private TradersChoice tradersResponse;

    public TradingResponse(int commodityType, double bidPrice, double askPrice, TradersChoice tradersResponse) {
        this.commodityType = commodityType;
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.tradersResponse = tradersResponse;

        if (bidPrice > askPrice) throw new RuntimeException("bid price should not be more than ask price");
    }

    public int getCommodityTypeId() {
        return commodityType;
    }

    public double getBidPrice() {
        return bidPrice;
    }

    public double getAskPrice() {
        return askPrice;
    }

    public TradersChoice getTradersResponse() {
        return tradersResponse;
    }


    @Override
    public String toString() {
        switch (tradersResponse) {
            case BUY: return "trader bought at " + askPrice;
            case SELL: return "trader sold at " + bidPrice;
            case NO_DEAL: return "no deal at bid: " + bidPrice + " / ask: " + askPrice;
            default: throw new RuntimeException("unknown choice");
        }
    }
}
