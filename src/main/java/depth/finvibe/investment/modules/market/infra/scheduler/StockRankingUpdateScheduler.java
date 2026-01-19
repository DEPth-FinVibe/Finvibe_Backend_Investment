package depth.finvibe.investment.modules.market.infra.scheduler;

import depth.finvibe.investment.modules.market.application.port.in.StockCommandUseCase;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockRankingUpdateScheduler {

    private final StockCommandUseCase stockCommandUseCase;

    @Scheduled(cron = "0 */10 * * * *")
    @SchedulerLock(
            name = "stockBulkUpsert",
            lockAtMostFor = "PT1M",
            lockAtLeastFor = "PT5S"
    )
    public void executeStockRankingUpdate() {
        stockCommandUseCase.renewStockCharts();
    }
}
