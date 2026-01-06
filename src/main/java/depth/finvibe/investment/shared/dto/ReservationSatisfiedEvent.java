package depth.finvibe.investment.shared.dto;

public record ReservationSatisfiedEvent (
    String tradeId,
    String userId,
    String type, // "BUY", "SELL"
    Double amount,
    Long price

){}
