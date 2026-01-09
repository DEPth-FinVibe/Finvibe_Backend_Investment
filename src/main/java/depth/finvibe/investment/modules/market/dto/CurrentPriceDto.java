package depth.finvibe.investment.modules.market.dto;

import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import jakarta.persistence.*;
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


        public static Response from(Long stockId, Timeframe timeframe, LocalDateTime at,
                                    BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close,
                                    BigDecimal prevDayChangePct, BigDecimal volume, BigDecimal value) {
            return Response.builder()
                    .stockId(stockId)
                    .timeframe(timeframe)
                    .at(at)
                    .open(open)
                    .high(high)
                    .low(low)
                    .close(close)
                    .prevDayChangePct(prevDayChangePct)
                    .volume(volume)
                    .value(value)
                    .build();
        }
    }
}
