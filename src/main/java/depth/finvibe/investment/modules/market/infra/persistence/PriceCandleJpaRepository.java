package depth.finvibe.investment.modules.market.infra.persistence;

import depth.finvibe.investment.modules.market.domain.PriceCandle;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceCandleJpaRepository extends JpaRepository<PriceCandle, Long> {
    List<PriceCandle> findByStockIdAndTimeframeAndAtBetweenOrderByAtAsc(
            Long stockId,
            Timeframe timeframe,
            LocalDateTime startAt,
            LocalDateTime endAt
    );
}
