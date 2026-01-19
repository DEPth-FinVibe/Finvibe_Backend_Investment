package depth.finvibe.investment.modules.market.infra.scheduler;

import depth.finvibe.investment.modules.market.application.port.in.StockCommandUseCase;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockBulkUpsertScheduler {

    private final StockCommandUseCase stockCommandUseCase;

    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(
            name = "stockBulkUpsert",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT30S"
    )
    public void executeStockBulkUpsert() {
        stockCommandUseCase.bulkUpsertStocks();
    }
}
