package depth.finvibe.investment.modules.asset.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.investment.modules.asset.application.port.out.PortfolioGroupRepository;
import depth.finvibe.investment.modules.asset.application.port.out.UserProfitSnapshotRepository;
import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;
import depth.finvibe.investment.modules.asset.domain.UserProfitSnapshotDaily;
import depth.finvibe.investment.modules.asset.domain.UserProfitSnapshotDailyId;

@Service
@RequiredArgsConstructor
public class UserProfitSnapshotService {
  private final PortfolioGroupRepository portfolioGroupRepository;
  private final UserProfitSnapshotRepository userProfitSnapshotRepository;

  @Transactional
  public void saveDailySnapshot(LocalDate snapshotDate) {
    if (snapshotDate == null) {
      return;
    }

    List<PortfolioGroup> portfolios = portfolioGroupRepository.findAllWithAssets();
    if (portfolios == null || portfolios.isEmpty()) {
      return;
    }

    Map<UUID, List<PortfolioGroup>> portfoliosByUser = portfolios.stream()
      .collect(Collectors.groupingBy(PortfolioGroup::getUserId));

    List<UserProfitSnapshotDaily> snapshots = new ArrayList<>();
    for (Map.Entry<UUID, List<PortfolioGroup>> entry : portfoliosByUser.entrySet()) {
      UserProfitSummary summary = calculateUserProfitSummary(entry.getValue());
      if (summary.hasAssets()) {
        UserProfitSnapshotDailyId id = new UserProfitSnapshotDailyId(entry.getKey(), snapshotDate);
        snapshots.add(UserProfitSnapshotDaily.create(
          id,
          summary.totalCurrentValue(),
          summary.totalProfitLoss(),
          summary.totalReturnRate()
        ));
      }
    }

    userProfitSnapshotRepository.saveAll(snapshots);
  }

  private UserProfitSummary calculateUserProfitSummary(List<PortfolioGroup> portfolios) {
    boolean hasAssets = portfolios.stream()
      .flatMap(portfolio -> portfolio.getAssets().stream())
      .findAny()
      .isPresent();

    if (!hasAssets) {
      return new UserProfitSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false);
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

    return new UserProfitSummary(totalCurrentValue, totalProfitLoss, totalReturnRate, true);
  }

  private BigDecimal calculateReturnRate(BigDecimal profitLoss, BigDecimal purchaseAmount) {
    if (purchaseAmount.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    return profitLoss
      .divide(purchaseAmount, 4, RoundingMode.HALF_UP)
      .multiply(BigDecimal.valueOf(100));
  }

  private record UserProfitSummary(
    BigDecimal totalCurrentValue,
    BigDecimal totalProfitLoss,
    BigDecimal totalReturnRate,
    boolean hasAssets
  ) {
  }
}
