package depth.finvibe.investment.modules.trade.domain;

import depth.finvibe.investment.modules.trade.domain.enums.MarketType;
import depth.finvibe.investment.modules.trade.domain.enums.TradeType;
import depth.finvibe.investment.modules.trade.domain.enums.TransactionType;
import depth.finvibe.investment.modules.trade.domain.error.TradeErrorCode;
import depth.finvibe.investment.shared.domain.TimeStampedBaseEntity;
import depth.finvibe.investment.shared.error.DomainException;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Trade extends TimeStampedBaseEntity{

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private MarketType marketType;

    private Long stockId;

    private Double amount;

    private Long price;

    private Long portfolioId;

    private UUID userId;

    private TransactionType transactionType;

    private TradeType tradeType;

    public static Trade create(MarketType marketType, Long stockId, Double amount, Long price,
                               Long portfolioId, UUID userId, TransactionType transactionType,
                               TradeType tradeType) {
        return Trade.builder()
                .marketType(marketType)
                .stockId(stockId)
                .amount(amount)
                .price(price)
                .portfolioId(portfolioId)
                .userId(userId)
                .transactionType(transactionType)
                .tradeType(tradeType)
                .build();
    }

   public void cancel(){
        if (this.tradeType == TradeType.CANCELLED) {
            throw new DomainException(TradeErrorCode.ALREADY_CANCELLED_TRADE);
        }
        if(this.tradeType != TradeType.RESERVED){
            throw new DomainException(TradeErrorCode.CANNOT_CANCEL_NON_RESERVED_TRADE);
        }
        this.tradeType = TradeType.CANCELLED;
    }
}
