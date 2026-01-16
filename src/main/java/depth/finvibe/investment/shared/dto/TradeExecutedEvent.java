package depth.finvibe.investment.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeExecutedEvent {
    private String tradeId;
    private String userId;
    private String type; // "BUY", "SELL"
    private BigDecimal amount;
    private BigDecimal price;
    private Long stockId;
    private String name;
    private String currency;
    private Long portfolioId;
}
