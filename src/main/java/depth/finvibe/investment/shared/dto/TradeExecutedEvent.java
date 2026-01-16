package depth.finvibe.investment.shared.dto;

public record TradeExecutedEvent (
        String tradeId,
        Long stockId,
        String userId,
        String type, // "BUY", "SELL"
        Double amount,
        Long price
) {}
