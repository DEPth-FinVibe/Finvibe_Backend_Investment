package depth.finvibe.investment.modules.market.domain;

import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "price_candle",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"stock_id", "timeframe", "at"})
        },
        indexes = {
                @Index(name = "idx_candle_stock_time", columnList = "stock_id,timeframe,at")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Getter
public class PriceCandle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // JPA 식별자 (기술적 PK)

    @Column(name = "stock_id", nullable = false)
    private Long stockId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Timeframe timeframe;

    @Column(nullable = false)
    private LocalDateTime at;

    @Column(nullable = false)
    private BigDecimal open;

    @Column(nullable = false)
    private BigDecimal high;

    @Column(nullable = false)
    private BigDecimal low;

    @Column(nullable = false)
    private BigDecimal close;

    @Column(nullable = false)
    private BigDecimal prevDayChangePct;

    @Column(nullable = false)
    private BigDecimal volume;

    @Column(nullable = false)
    private BigDecimal value;

    public static PriceCandle create(Long stockId, Timeframe timeframe, LocalDateTime at, BigDecimal open, BigDecimal high,
                       BigDecimal low, BigDecimal close, BigDecimal prevDayChangePct, BigDecimal volume, BigDecimal value) {
        return PriceCandle.builder()
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PriceCandle that)) return false;

        // 비즈니스 식별자만 비교 (stockId, timeframe, at)
        return Objects.equals(stockId, that.stockId)
                && timeframe == that.timeframe
                && Objects.equals(at, that.at);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stockId, timeframe, at);
    }

}
