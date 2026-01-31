package depth.finvibe.investment.modules.asset.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import depth.finvibe.investment.modules.asset.application.event.AllUserProfitRatesUpdatedEvent;
import depth.finvibe.investment.modules.asset.application.port.in.ProfitCalculationUseCase;
import depth.finvibe.investment.modules.asset.application.port.out.PortfolioGroupRepository;
import depth.finvibe.investment.modules.asset.application.port.out.UserProfitRankingData;
import depth.finvibe.investment.modules.asset.domain.Asset;
import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;
import depth.finvibe.investment.modules.asset.infra.client.MarketInternalClient;
import depth.finvibe.investment.shared.dto.BatchPriceSnapshot;

@Service
@RequiredArgsConstructor
public class ProfitCalculationService implements ProfitCalculationUseCase {
  private final PortfolioGroupRepository portfolioGroupRepository;
  private final MarketInternalClient marketInternalClient;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public void recalculateAllProfits(List<Long> updatedStockIds) {
    if (updatedStockIds == null || updatedStockIds.isEmpty()) {
      return;
    }

    List<PortfolioGroup> portfolios = portfolioGroupRepository.findAllByStockIdsWithAssets(updatedStockIds);
    if (portfolios.isEmpty()) {
      List<PortfolioGroup> allPortfolios = portfolioGroupRepository.findAllWithAssets();
      publishUserProfitRatesUpdatedEvent(allPortfolios);
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

    List<PortfolioGroup> allPortfolios = portfolioGroupRepository.findAllWithAssets();
    publishUserProfitRatesUpdatedEvent(allPortfolios);
  }

  private void publishUserProfitRatesUpdatedEvent(List<PortfolioGroup> portfolios) {
    if (portfolios == null || portfolios.isEmpty()) {
      return;
    }

    Map<UUID, List<PortfolioGroup>> portfoliosByUser = portfolios.stream()
      .collect(Collectors.groupingBy(PortfolioGroup::getUserId));

    List<UserProfitRankingData> rankings = buildRankings(portfoliosByUser);
    AllUserProfitRatesUpdatedEvent event = AllUserProfitRatesUpdatedEvent.builder()
      .rankings(rankings)
      .calculatedAt(LocalDateTime.now())
      .build();
    eventPublisher.publishEvent(event);
  }

  private List<UserProfitRankingData> buildRankings(Map<UUID, List<PortfolioGroup>> portfoliosByUser) {
    List<UserProfitRankingData> rankings = new ArrayList<>();
    for (Map.Entry<UUID, List<PortfolioGroup>> entry : portfoliosByUser.entrySet()) {
      UserProfitSummary summary = calculateUserProfitSummary(entry.getValue());
      if (summary.hasAssets()) {
        rankings.add(new UserProfitRankingData(
          entry.getKey(),
          summary.totalReturnRate(),
          summary.totalProfitLoss()
        ));
      }
    }
    return rankings;
  }

  private UserProfitSummary calculateUserProfitSummary(List<PortfolioGroup> portfolios) {
    boolean hasAssets = portfolios.stream()
      .flatMap(portfolio -> portfolio.getAssets().stream())
      .findAny()
      .isPresent();

    if (!hasAssets) {
      return new UserProfitSummary(BigDecimal.ZERO, BigDecimal.ZERO, false);
    }

    BigDecimal totalCurrentValue = portfolios.stream()
      .map(PortfolioGroup::getValuation)
      .filter(Objects::nonNull)
      .map(valuation -> Objects.requireNonNullElse(valuation.getTotalCurrentValue(), BigDecimal.ZERO))
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalProfitLoss = portfolios.stream()
      .map(PortfolioGroup::getValuation)
      .filter(Objects::nonNull)
      .map(valuation -> Objects.requireNonNullElse(valuation.getTotalProfitLoss(), BigDecimal.ZERO))
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal purchaseAmount = totalCurrentValue.subtract(totalProfitLoss);
    BigDecimal totalReturnRate = calculateReturnRate(totalProfitLoss, purchaseAmount);

    return new UserProfitSummary(totalReturnRate, totalProfitLoss, true);
  }

  private BigDecimal calculateReturnRate(BigDecimal profitLoss, BigDecimal purchaseAmount) {
    if (purchaseAmount.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }

    return profitLoss
      .divide(purchaseAmount, 4, RoundingMode.HALF_UP)
      .multiply(BigDecimal.valueOf(100));
  }

  private record UserProfitSummary(BigDecimal totalReturnRate, BigDecimal totalProfitLoss, boolean hasAssets) {
  }
}
