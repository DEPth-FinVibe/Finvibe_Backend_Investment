package depth.finvibe.investment.modules.market.infra.scheduler;

import depth.finvibe.investment.modules.market.application.port.in.CategoryCommandUseCase;
import depth.finvibe.investment.modules.market.application.port.in.StockCommandUseCase;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.Category;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "market.init", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InitialMarketDataRunner implements CommandLineRunner {

    private static final String LOCK_NAME = "initial-market-data-runner";
    private static final Duration LOCK_AT_MOST_FOR = Duration.ofMinutes(10);
    private static final Duration LOCK_AT_LEAST_FOR = Duration.ofSeconds(5);
    private static final String CATEGORY_RESOURCE_PATH = "seed/standard-industry-mid-categories.json";

    private final LockProvider lockProvider;
    private final CategoryCommandUseCase categoryCommandUseCase;
    private final StockCommandUseCase stockCommandUseCase;
    private final StockRepository stockRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) {
        LockingTaskExecutor executor = new DefaultLockingTaskExecutor(lockProvider);
        executor.executeWithLock((Runnable) this::initializeMarketData,
                new LockConfiguration(Instant.now(), LOCK_NAME, LOCK_AT_MOST_FOR, LOCK_AT_LEAST_FOR));
    }

    private void initializeMarketData() {
        if (!categoryCommandUseCase.existsAny()) {
            List<Category> categories = loadCategorySeeds().stream()
                    .map(seed -> Category.builder()
                            .code(seed.code())
                            .name(seed.name())
                            .build())
                    .toList();
            categoryCommandUseCase.bulkInsert(categories);
        }

        if (stockRepository.existsAny()) {
            return;
        }
        stockCommandUseCase.bulkUpsertStocks();
        stockCommandUseCase.renewStockCharts();
    }

    private List<CategorySeed> loadCategorySeeds() {
        Resource resource = new ClassPathResource(CATEGORY_RESOURCE_PATH);
        try {
            return objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read category seed resource: " + CATEGORY_RESOURCE_PATH, e);
        }
    }

    private record CategorySeed(String code, String name) {}
}
