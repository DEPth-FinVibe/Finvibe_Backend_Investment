    package depth.finvibe.investment.modules.asset.dto;

    import depth.finvibe.investment.modules.asset.domain.Currency;

    import java.math.BigDecimal;

    public record TradeExecutedEvent (
            Long stockId,
            BigDecimal amount,
            BigDecimal stockPrice,
            String name,
            Currency currency,
            Long portfolioId,
            String userId,
            String type // "BUY", "SELL"
    ) {}
