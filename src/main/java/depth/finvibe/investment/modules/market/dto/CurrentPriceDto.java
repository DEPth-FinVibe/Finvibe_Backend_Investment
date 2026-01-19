package depth.finvibe.investment.modules.market.dto;

import depth.finvibe.investment.modules.market.domain.CurrentPrice;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CurrentPriceDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {

        private Long stockId;
        private Timeframe timeframe;
        private LocalDateTime at;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private BigDecimal prevDayChangePct;
        private BigDecimal volume;
        private BigDecimal value;

        public static Response from(CurrentPrice currentPrice) {
            return Response.builder()
                    .stockId(currentPrice.getStockId())
                    .at(currentPrice.getAt())
                    .open(currentPrice.getOpen())
                    .high(currentPrice.getHigh())
                    .low(currentPrice.getLow())
                    .close(currentPrice.getClose())
                    .prevDayChangePct(currentPrice.getPrevDayChangePct())
                    .volume(currentPrice.getVolume())
                    .value(currentPrice.getValue())
                    .build();
        }
    }
}
