package depth.finvibe.investment.modules.market.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public record CurrentPrice(Long stockId, LocalDateTime at, BigDecimal price, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close,
                           BigDecimal prevDayChangePct, Long volume, Long value) {

}