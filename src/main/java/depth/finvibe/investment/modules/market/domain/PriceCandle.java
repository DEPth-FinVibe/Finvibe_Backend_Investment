package depth.finvibe.investment.modules.market.domain;

import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

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
    private Long volume;

    @Column(nullable = false)
    private Long value;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PriceCandle that)) return false;
        return Objects.equals(stockId, that.stockId)
                && timeframe == that.timeframe
                && Objects.equals(at, that.at);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stockId, timeframe, at);
    }
}
