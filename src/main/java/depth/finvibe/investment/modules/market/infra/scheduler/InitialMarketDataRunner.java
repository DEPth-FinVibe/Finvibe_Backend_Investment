package depth.finvibe.investment.modules.market.infra.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import depth.finvibe.investment.modules.market.application.port.in.CategoryCommandUseCase;
import depth.finvibe.investment.modules.market.application.port.in.StockCommandUseCase;
import depth.finvibe.investment.modules.market.application.BatchPriceUpdateService;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.application.port.out.StockThemeRepository;
import depth.finvibe.investment.modules.market.domain.Category;
import depth.finvibe.investment.modules.market.domain.MarketHours;
import depth.finvibe.investment.modules.market.domain.enums.MarketStatus;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "market.init", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InitialMarketDataRunner implements CommandLineRunner {

    private static final String LOCK_NAME = "initial-market-data-runner";
    private static final Duration LOCK_AT_MOST_FOR = Duration.ofMinutes(10);
    private static final Duration LOCK_AT_LEAST_FOR = Duration.ofSeconds(5);
    private static final String FALLBACK_CATEGORY_NAME = "기타";

    private final LockProvider lockProvider;
    private final CategoryCommandUseCase categoryCommandUseCase;
    private final StockCommandUseCase stockCommandUseCase;
    private final BatchPriceUpdateService batchPriceUpdateService;
    private final StockRepository stockRepository;
    private final StockThemeRepository stockThemeRepository;

    @Override
    public void run(String... args) {
        LockingTaskExecutor executor = new DefaultLockingTaskExecutor(lockProvider);
        executor.executeWithLock((Runnable) this::initializeMarketData,
                new LockConfiguration(Instant.now(), LOCK_NAME, LOCK_AT_MOST_FOR, LOCK_AT_LEAST_FOR));
    }

    private void initializeMarketData() {
        if (!categoryCommandUseCase.existsAny()) {
            List<Category> categories = loadCategoryThemes().stream()
                    .map(theme -> Category.builder()
                            .name(theme)
                            .build())
                    .toList();
            categoryCommandUseCase.bulkInsert(categories);
        }

        if (!stockRepository.existsAny()) {
            log.info("어플리케이션 초기화 작업을 위해 주식 데이터를 최초로 적재합니다.");

            stockCommandUseCase.bulkUpsertStocks();
            stockCommandUseCase.renewStockCharts();
        }

        runBatchPriceUpdateIfClosedAndMissing();
    }

    private void runBatchPriceUpdateIfClosedAndMissing() {
        if (MarketHours.getCurrentStatus() == MarketStatus.OPEN) {
            log.info("장이 열려 있어 시작 시 배치 시세 보정은 건너뜁니다.");
            return;
        }

        if (!batchPriceUpdateService.hasMissingBatchPricesByKey()) {
            log.info("장이 닫혀 있고 누락된 배치 시세 키가 없어 시작 시 보정을 건너뜁니다.");
            return;
        }

        log.info("장이 닫혀 있고 누락된 배치 시세 키가 있어 시작 시 배치 시세 보정을 실행합니다.");
        batchPriceUpdateService.updateHoldingStockPrices();
    }

    private List<String> loadCategoryThemes() {
        Set<String> themes = stockThemeRepository.findSymbolToThemeMap().values().stream()
                .map(theme -> theme == null ? "" : theme.trim())
                .filter(theme -> !theme.isBlank())
                .collect(Collectors.toSet());
        themes.add(FALLBACK_CATEGORY_NAME);
        return themes.stream()
                .sorted()
                .toList();
    }
}
