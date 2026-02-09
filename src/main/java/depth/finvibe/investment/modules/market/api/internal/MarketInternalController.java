package depth.finvibe.investment.modules.market.api.internal;

import java.util.List;

import depth.finvibe.investment.modules.market.application.port.in.MarketQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import depth.finvibe.investment.modules.market.application.port.in.BatchPriceQueryUseCase;
import depth.finvibe.investment.modules.market.application.port.in.CategoryQueryUseCase;
import depth.finvibe.investment.modules.market.dto.CategoryInternalDto;
import depth.finvibe.investment.shared.dto.BatchPriceSnapshot;

@RestController
@RequestMapping("/internal/market")
@RequiredArgsConstructor
public class MarketInternalController {
    private final BatchPriceQueryUseCase batchPriceQueryService;
    private final CategoryQueryUseCase categoryQueryUseCase;
    private final MarketQueryUseCase marketQueryService;

    @GetMapping("/batch-prices")
    public List<BatchPriceSnapshot> getBatchPrices(@RequestParam List<Long> stockIds) {
        return batchPriceQueryService.getBatchPrices(stockIds);
    }

    @GetMapping("/stocks/{stockId}/name")
    public String getStockName(@PathVariable Long stockId) {
        return marketQueryService.getStockNameById(stockId);
    }

    @GetMapping("/categories")
    public List<CategoryInternalDto.Response> getCategories() {
        return categoryQueryUseCase.getAllCategoriesForInternal();
    }
}
