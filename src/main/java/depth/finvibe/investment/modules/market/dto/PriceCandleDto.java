package depth.finvibe.investment.modules.market.dto;

import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PriceCandleDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {

        private BigDecimal open;
        private BigDecimal close;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal volume;
        private BigDecimal value;
        private Long stockId;
        private Timeframe timeframe;
        private LocalDateTime at;
        private BigDecimal prevDayChangePct;

        public static Response from(Long stockId, BigDecimal open, BigDecimal close, BigDecimal high, BigDecimal low,
                                    BigDecimal volume, BigDecimal value, Timeframe timeframe,
                                    LocalDateTime at,  BigDecimal prevDayChangePct) {
            return Response.builder()
                    .open(open)
                    .close(close)
                    .high(high)
                    .low(low)
                    .volume(volume)
                    .value(value)
                    .stockId(stockId)
                    .timeframe(timeframe)
                    .at(at)
                    .prevDayChangePct(prevDayChangePct)
                    .build();
        }
    }
}
