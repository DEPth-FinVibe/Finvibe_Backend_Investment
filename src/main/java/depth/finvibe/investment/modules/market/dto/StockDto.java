package depth.finvibe.investment.modules.market.dto;

import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.domain.enums.MarketType;
import depth.finvibe.investment.modules.market.domain.enums.RankType;
import lombok.*;

public class StockDto {
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long stockId;
        private String symbol;
        private String name;
        private Long categoryId;

        public static Response from(Stock stock) {
            return Response.builder()
                    .stockId(stock.getId())
                    .name(stock.getName())
                    .symbol(stock.getSymbol())
                    .categoryId(stock.getCategoryId())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopStockResponse{
        private String symbol;

        public static TopStockResponse from(Stock stock){
            return TopStockResponse.builder()
                    .symbol(stock.getSymbol())
                    .build();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class RealMarketStockResponse {
        private String symbol;
        private String name;
        private MarketType marketType;
        private String typeCode; // 표준산업분류코드 (중분류)
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class RankingResponse {
        private String symbol;
        private RankType rankType;
        private Integer rank;
    }
}
