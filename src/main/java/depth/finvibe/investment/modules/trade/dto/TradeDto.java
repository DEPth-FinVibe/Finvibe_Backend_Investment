package depth.finvibe.investment.modules.trade.dto;

import depth.finvibe.investment.modules.trade.domain.Trade;
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
    @AllArgsConstructor
    @Builder
    public static class TransactionRequest {
        private MarketType marketType;
        private Long stockId;
        private Double amount;
        private Long price;
        private Long portfolioId;
        private UUID userId;
        private TradeType tradeType;
        private TransactionType transactionType;
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

        public static TradeResponse from(Trade trade) {
            return TradeResponse.builder()
                    .tradeId(trade.getId())
                    .marketType(trade.getMarketType())
                    .stockId(trade.getStockId())
                    .amount(trade.getAmount())
                    .price(trade.getPrice())
                    .portfolioId(trade.getPortfolioId())
                    .userId(trade.getUserId())
                    .tradeType(trade.getTradeType())
                    .transactionType(trade.getTransactionType())
                    .build();
        }
    }
}
