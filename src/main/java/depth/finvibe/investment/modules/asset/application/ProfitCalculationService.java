package depth.finvibe.investment.modules.asset.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import depth.finvibe.investment.modules.asset.application.port.out.PortfolioGroupRepository;
import depth.finvibe.investment.modules.asset.domain.Asset;
import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;
import depth.finvibe.investment.modules.asset.infra.client.MarketInternalClient;
import depth.finvibe.investment.shared.dto.BatchPriceSnapshot;

@Service
@RequiredArgsConstructor
public class ProfitCalculationService {
    private final PortfolioGroupRepository portfolioGroupRepository;
    private final MarketInternalClient marketInternalClient;

    @Transactional
    public void recalculateAllProfits(List<Long> updatedStockIds) {
        if (updatedStockIds == null || updatedStockIds.isEmpty()) {
            return;
        }

        List<PortfolioGroup> portfolios = portfolioGroupRepository.findAllByStockIdsWithAssets(updatedStockIds);
        if (portfolios.isEmpty()) {
            return;
        }

        List<Long> stockIds = portfolios.stream()
                .flatMap(portfolio -> portfolio.getAssets().stream())
                .map(Asset::getStockId)
                .distinct()
                .toList();

        if (stockIds.isEmpty()) {
            return;
        }

        List<BatchPriceSnapshot> batchPrices = marketInternalClient.getBatchPrices(stockIds);
        if (batchPrices == null || batchPrices.isEmpty()) {
            return;
        }

        Map<Long, BigDecimal> priceByStockId = batchPrices.stream()
                .collect(Collectors.toMap(BatchPriceSnapshot::getStockId, BatchPriceSnapshot::getPrice));

        for (PortfolioGroup portfolio : portfolios) {
            for (Asset asset : portfolio.getAssets()) {
                BigDecimal currentPrice = priceByStockId.get(asset.getStockId());
                if (currentPrice != null) {
                    asset.updateValuation(currentPrice);
                }
            }
            portfolio.recalculateValuation();
        }
    }
}
