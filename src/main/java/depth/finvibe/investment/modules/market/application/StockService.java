package depth.finvibe.investment.modules.market.application;

import depth.finvibe.investment.modules.market.application.port.in.StockCommandUseCase;
import depth.finvibe.investment.modules.market.application.port.out.CategoryRepository;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.Category;
import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.dto.StockDto;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class StockService implements StockCommandUseCase {

    private final StockRepository stockRepository;
    private final CategoryRepository categoryRepository;

    private static final String FALLBACK_CATEGORY_CODE = "0000";

    @Override
    @Transactional
    public void bulkUpsertStocks(List<StockDto.CreateRequest> stockCreateRequests) {
        List<Category> allCategories = categoryRepository.findAll();

        List<Stock> stocksToUpsert = stockCreateRequests.stream()
            .map(req -> {
                Category category = seekMatchingCategories(req, allCategories);
                return createStockFrom(req, category);
            })
            .toList();

        stockRepository.bulkUpsertStocks(stocksToUpsert);
    }

    private static @NonNull Category seekMatchingCategories(StockDto.CreateRequest request, List<Category> allCategories) {
        Category fallbackCategory = allCategories.stream()
            .filter(cat -> cat.getCode().equals(FALLBACK_CATEGORY_CODE)).findAny().orElseThrow();

        return allCategories.stream()
                .filter(cat -> cat.getCode().equals(request.getRawCategoryCode())).findAny()
            .orElse(fallbackCategory);
    }

    private static Stock createStockFrom(StockDto.CreateRequest request, Category category) {
        return Stock.builder()
            .symbol(request.getSymbol())
            .name(request.getName())
            .categoryId(category.getId())
            .build();
    }

}
