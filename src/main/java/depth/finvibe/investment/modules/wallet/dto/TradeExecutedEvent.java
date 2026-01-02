package depth.finvibe.investment.modules.wallet.dto;

public record TradeExecutedEvent (
        String tradeId,
        String userId,
        String type, // "BUY", "SELL"
        Long amount,
        Long price
) {}
