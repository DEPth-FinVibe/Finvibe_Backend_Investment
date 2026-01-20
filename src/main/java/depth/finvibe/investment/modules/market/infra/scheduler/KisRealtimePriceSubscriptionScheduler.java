package depth.finvibe.investment.modules.market.infra.scheduler;

import depth.finvibe.investment.modules.market.application.port.out.RealtimeStockIndexRepository;
import depth.finvibe.investment.modules.market.infra.websocket.kis.KisRealtimePriceSubscriber;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KisRealtimePriceSubscriptionScheduler {
    private final RealtimeStockIndexRepository realtimeStockIndexRepository;
    private final KisRealtimePriceSubscriber subscriber;

    @Scheduled(fixedDelayString = "${market.kis.websocket.sync-interval-ms:5000}")
    @SchedulerLock(
            name = "kisRealtimePriceSubscriptionSync",
            lockAtMostFor = "PT30S",
            lockAtLeastFor = "PT1S"
    )
    public void syncRealtimeSubscriptions() {
        subscriber.syncSubscriptions(realtimeStockIndexRepository.findActiveStockIds());
    }
}
