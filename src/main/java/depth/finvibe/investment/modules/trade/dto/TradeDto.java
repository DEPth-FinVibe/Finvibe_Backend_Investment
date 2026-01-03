package depth.finvibe.investment.modules.trade.dto;

import depth.finvibe.investment.modules.trade.domain.enums.MarketType;
import depth.finvibe.investment.modules.trade.domain.enums.TradeType;
import depth.finvibe.investment.modules.trade.domain.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

public class TradeDto {

    @Getter
    @NoArgsConstructor
    public static class TransactionRequest {
        private MarketType marketType;
        private Long stockId;
        private Double amount;
        private Long portfolioId;
        private UUID userId;
        private TradeType tradeType;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TradeResponse {
        private Long tradeId;
        private MarketType marketType;
        private Long stockId;
        private Double amount;
        private Long price;
        private Long portfolioId;
        private UUID userId;
        private TradeType tradeType;
        private TransactionType transactionType;

        public static TradeResponse from(Long tradeId, MarketType marketType, Long stockId, Double amount,
                                         Long price, Long portfolioId, UUID userId, TradeType tradeType, TransactionType transactionType) {
            return TradeResponse.builder()
                    .tradeId(tradeId)
                    .marketType(marketType)
                    .stockId(stockId)
                    .amount(amount)
                    .price(price)
                    .portfolioId(portfolioId)
                    .userId(userId)
                    .tradeType(tradeType)
                    .transactionType(transactionType)
                    .build();
        }
    }
}
