package depth.finvibe.investment.modules.market.infra.persistence;

import depth.finvibe.investment.modules.market.application.port.out.PriceCandleRepository;
import depth.finvibe.investment.modules.market.domain.PriceCandle;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class PriceCandleRepositoryImpl implements PriceCandleRepository {

    private final PriceCandleJpaRepository jpaRepository;

    @Override
    public List<PriceCandle> findExisting(Long stockId, LocalDateTime startTime, Timeframe timeframe, Integer count) {
        LocalDateTime alignedStartTime = alignStartTime(startTime, timeframe);
        LocalDateTime endTime = calculateEndTime(alignedStartTime, timeframe, count);
        return jpaRepository.findByStockIdAndTimeframeAndAtBetweenOrderByAtAsc(stockId, timeframe, alignedStartTime, endTime);
    }

    @Override
    @Transactional
    public void saveAll(List<PriceCandle> fetchedResult) {
        jpaRepository.saveAll(fetchedResult);
    }

    private LocalDateTime calculateEndTime(LocalDateTime startTime, Timeframe timeframe, Integer count) {
        int lastIndex = Math.max(count - 1, 0);
        return switch (timeframe) {
            case DAY -> startTime.plusDays(lastIndex).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case HOUR -> startTime.plusHours(lastIndex).withMinute(0).withSecond(0).withNano(0);
            case MINUTE -> startTime.plusMinutes(lastIndex).withSecond(0).withNano(0);
        };
    }

    private LocalDateTime alignStartTime(LocalDateTime startTime, Timeframe timeframe) {
        return switch (timeframe) {
            case DAY -> startTime.withHour(0).withMinute(0).withSecond(0).withNano(0);
            case HOUR -> startTime.withMinute(0).withSecond(0).withNano(0);
            case MINUTE -> startTime.withSecond(0).withNano(0);
        };
    }
}

