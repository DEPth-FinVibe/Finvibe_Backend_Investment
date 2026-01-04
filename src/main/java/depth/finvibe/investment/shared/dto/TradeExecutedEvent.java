package depth.finvibe.investment.shared.dto;

public record TradeExecutedEvent (
        String tradeId,
        String userId,
        String type, // "BUY", "SELL"
        Double amount,
        Long price
) {}
