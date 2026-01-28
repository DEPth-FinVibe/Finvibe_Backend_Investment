package depth.finvibe.investment.modules.market.api.internal;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import depth.finvibe.investment.modules.market.application.BatchPriceQueryService;
import depth.finvibe.investment.modules.market.domain.BatchUpdatePrice;
import depth.finvibe.investment.shared.dto.BatchPriceSnapshot;

@RestController
@RequestMapping("/internal/market")
@RequiredArgsConstructor
public class MarketInternalController {
    private final BatchPriceQueryService batchPriceQueryService;

    @GetMapping("/batch-prices")
    public List<BatchPriceSnapshot> getBatchPrices(@RequestParam List<Long> stockIds) {
        return batchPriceQueryService.getBatchPrices(stockIds).stream()
                .map(this::toSnapshot)
                .toList();
    }

    private BatchPriceSnapshot toSnapshot(BatchUpdatePrice price) {
        return BatchPriceSnapshot.builder()
                .stockId(price.getStockId())
                .price(price.getPrice())
                .at(price.getAt())
                .build();
    }
}
