package depth.finvibe.investment.modules.market.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public record CurrentPrice(Long stockId, LocalDateTime at, BigDecimal price, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close,
                           float prevDayChangePct, Long volume, Long value) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CurrentPrice that = (CurrentPrice) o;
        return Float.compare(that.prevDayChangePct, prevDayChangePct) == 0 &&
                Objects.equals(stockId, that.stockId) &&
                Objects.equals(at, that.at) &&
                Objects.equals(price, that.price) &&
                Objects.equals(open, that.open) &&
                Objects.equals(high, that.high) &&
                Objects.equals(low, that.low) &&
                Objects.equals(close, that.close) &&
                Objects.equals(volume, that.volume) &&
                Objects.equals(value, that.value);
    }

}