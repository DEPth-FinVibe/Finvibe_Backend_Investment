package depth.finvibe.investment.modules.market.infra.scheduler;

import depth.finvibe.investment.modules.market.application.IndexMinuteCandleCacheService;
import depth.finvibe.investment.modules.market.domain.MarketHours;
import depth.finvibe.investment.modules.market.domain.enums.MarketStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexMinuteCandleCacheScheduler {

    private final IndexMinuteCandleCacheService indexMinuteCandleCacheService;

    @Scheduled(cron = "${market.index-cache.cron:0 * * * * *}")
    @SchedulerLock(
            name = "indexMinuteCandleCache",
            lockAtMostFor = "PT1M",
            lockAtLeastFor = "PT5S"
    )
    public void cacheIndexMinuteCandles() {
        if (MarketHours.getCurrentStatus() != MarketStatus.OPEN) {
            log.debug("Skipping index minute candle cache - outside market hours");
            return;
        }

        try {
            indexMinuteCandleCacheService.cacheLatestMinuteCandles();
        } catch (Exception e) {
            log.error("Failed to cache index minute candles", e);
        }
    }
}
