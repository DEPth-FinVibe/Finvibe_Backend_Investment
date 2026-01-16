package depth.finvibe.investment.modules.market.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class StockDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long stockId;
        private String name;
        private String symbol;
        private Long categoryId;

        public static Response from(Long stockId, String name, String symbol, Long categoryId) {
            return Response.builder()
                    .stockId(stockId)
                    .name(name)
                    .symbol(symbol)
                    .categoryId(categoryId)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NewStock {
        private String name;
        private String symbol;
        private Long categoryId;
    }
}
