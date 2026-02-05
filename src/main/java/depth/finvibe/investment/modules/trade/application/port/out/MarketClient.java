package depth.finvibe.investment.modules.trade.application.port.out;

public interface MarketClient {
    boolean isMarketOpen();

    Long getCurrentPrice(Long stockId);
}
