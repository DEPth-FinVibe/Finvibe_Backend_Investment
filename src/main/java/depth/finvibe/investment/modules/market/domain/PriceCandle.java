package depth.finvibe.investment.modules.market.domain;

import depth.finvibe.investment.modules.market.domain.enums.Timeframe;

import java.time.LocalDateTime;
import java.util.Objects;

public record PriceCandle(Long stockId, Timeframe timeframe, LocalDateTime at, Long open, Long high, Long low,
                          Long close, float prevDayChangePct, Long volume, Long value) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PriceCandle that = (PriceCandle) o;
        return Float.compare(that.prevDayChangePct, prevDayChangePct) == 0 &&
                Objects.equals(stockId, that.stockId) &&
                Objects.equals(timeframe, that.timeframe) &&
                Objects.equals(at, that.at) &&
                Objects.equals(open, that.open) &&
                Objects.equals(high, that.high) &&
                Objects.equals(low, that.low) &&
                Objects.equals(close, that.close) &&
                Objects.equals(volume, that.volume) &&
                Objects.equals(value, that.value);
    }

}