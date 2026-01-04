package depth.finvibe.investment.shared.dto;

public record TradeExecutedEvent (
        String tradeId,
        String userId,
        String type, // "BUY", "SELL"
        Long amount,
        Long price
) {}
