package depth.finvibe.investment.modules.asset.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.investment.modules.asset.application.port.in.AssetCommandUseCase;
import depth.finvibe.investment.modules.asset.application.port.in.AssetQueryUseCase;
import depth.finvibe.investment.modules.asset.application.port.out.PortfolioGroupRepository;
import depth.finvibe.investment.modules.asset.application.port.out.TopHoldingStockCacheRepository;
import depth.finvibe.investment.modules.asset.domain.Asset;
import depth.finvibe.investment.modules.asset.domain.Money;
import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;
import depth.finvibe.investment.modules.asset.domain.error.AssetErrorCode;
import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;
import depth.finvibe.investment.modules.asset.dto.TopHoldingStockDto;
import depth.finvibe.investment.modules.wallet.application.port.in.WalletQueryUseCase;
import depth.finvibe.investment.shared.application.port.out.GamificationEventProducer;
import depth.finvibe.investment.shared.dto.MetricEventType;
import depth.finvibe.investment.shared.dto.UserMetricUpdatedEvent;
import depth.finvibe.investment.shared.error.DomainException;

@Service
@RequiredArgsConstructor
public class AssetService implements AssetCommandUseCase, AssetQueryUseCase {
    private static final BigDecimal INITIAL_ASSET_BASELINE = BigDecimal.valueOf(10_000_000L);

    private final PortfolioGroupRepository portfolioGroupRepository;
    private final GamificationEventProducer gamificationEventProducer;
    private final TopHoldingStockCacheRepository topHoldingStockCacheRepository;
    private final WalletQueryUseCase walletQueryUseCase;

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioGroupDto.AssetResponse> getAssetsByPortfolio(Long portfolioId, UUID requesterUserId) {
        PortfolioGroup portfolioGroup = findPortfolioGroupWithAssets(portfolioId);

        if (!portfolioGroup.getUserId().equals(requesterUserId)) {
            throw new DomainException(AssetErrorCode.ONLY_OWNER_CAN_VIEW_ASSETS);
        }

        return portfolioGroup.getAssets().stream()
                .map(PortfolioGroupDto.AssetResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioGroupDto.PortfolioGroupResponse> getPortfoliosByUser(UUID userId) {
        return portfolioGroupRepository.findAllByUserId(userId).stream()
                .map(PortfolioGroupDto.PortfolioGroupResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioGroupDto.PortfolioComparisonResponse> getPortfolioComparisons(UUID userId) {
        return portfolioGroupRepository.findAllByUserId(userId).stream()
                .map(PortfolioGroupDto.PortfolioComparisonResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isExistPortfolio(Long portfolioId, UUID userId) {
        return portfolioGroupRepository.existsByIdAndUserId(portfolioId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public TopHoldingStockDto.TopHoldingStockListResponse getTopHoldingStocks(UUID userId) {
        if (userId == null) {
            return TopHoldingStockDto.TopHoldingStockListResponse.builder()
                    .totalElements(0)
                    .items(List.of())
                    .build();
        }
        return topHoldingStockCacheRepository.find(userId)
                .orElseGet(() -> getTopHoldingStocksFromSource(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public PortfolioGroupDto.AssetAllocationResponse getAssetAllocation(UUID requesterUserId) {
        List<PortfolioGroup> portfolios = portfolioGroupRepository.findAllByUserIdWithAssets(requesterUserId);

        BigDecimal stockAmount = portfolios.stream()
                .flatMap(portfolio -> portfolio.getAssets().stream())
                .map(this::resolveAssetCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cashAmount = BigDecimal.valueOf(walletQueryUseCase.getWalletByUserId(requesterUserId).getBalance());
        BigDecimal totalAmount = cashAmount.add(stockAmount);
        BigDecimal changeAmount = totalAmount.subtract(INITIAL_ASSET_BASELINE);
        BigDecimal changeRate = changeAmount
                .divide(INITIAL_ASSET_BASELINE, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return PortfolioGroupDto.AssetAllocationResponse.builder()
                .cashAmount(cashAmount)
                .stockAmount(stockAmount)
                .totalAmount(totalAmount)
                .changeAmount(changeAmount)
                .changeRate(changeRate)
                .build();
    }

    private TopHoldingStockDto.TopHoldingStockListResponse getTopHoldingStocksFromSource(UUID userId) {
        List<PortfolioGroup> portfolios = portfolioGroupRepository.findAllByUserIdWithAssets(userId);
        Map<Long, HoldingSummary> summaryByStockId = new HashMap<>();

        for (PortfolioGroup portfolio : portfolios) {
            for (Asset asset : portfolio.getAssets()) {
                HoldingSummary existing = summaryByStockId.get(asset.getStockId());
                if (existing == null) {
                    summaryByStockId.put(asset.getStockId(), new HoldingSummary(asset.getName(), asset.getAmount()));
                } else {
                    existing.addAmount(asset.getAmount());
                }
            }
        }

        List<TopHoldingStockDto.TopHoldingStockResponse> items = summaryByStockId.entrySet().stream()
                .map(entry -> TopHoldingStockDto.TopHoldingStockResponse.builder()
                        .stockId(entry.getKey())
                        .name(entry.getValue().name())
                        .totalAmount(entry.getValue().totalAmount())
                        .build())
                .sorted(Comparator.comparing(TopHoldingStockDto.TopHoldingStockResponse::getTotalAmount)
                        .reversed()
                        .thenComparing(TopHoldingStockDto.TopHoldingStockResponse::getStockId))
                .limit(100)
                .toList();

        TopHoldingStockDto.TopHoldingStockListResponse response = TopHoldingStockDto.TopHoldingStockListResponse.builder()
                .totalElements(items.size())
                .items(items)
                .build();
        topHoldingStockCacheRepository.save(userId, response);
        return response;
    }

    private BigDecimal resolveAssetCurrentValue(Asset asset) {
        if (asset.getValuation() != null && asset.getValuation().getCurrentValue() != null) {
            return asset.getValuation().getCurrentValue();
        }
        if (asset.getTotalPrice() != null && asset.getTotalPrice().getAmount() != null) {
            return asset.getTotalPrice().getAmount();
        }
        return BigDecimal.ZERO;
    }

    @Override
    @Transactional
    public void registerAsset(Long portfolioId, PortfolioGroupDto.RegisterAssetRequest request, UUID requesterUserId) {
        HoldingMetricsSnapshot beforeSnapshot = getHoldingMetricsSnapshot(requesterUserId);

        PortfolioGroup foundPortfolioGroup = findPortfolioGroupWithAssets(portfolioId);

        Asset toRegister = toAssetEntity(request, requesterUserId);

        foundPortfolioGroup.register(toRegister, requesterUserId);

        HoldingMetricsSnapshot afterSnapshot = getHoldingMetricsSnapshot(requesterUserId);
        publishHoldingMetricsIfChanged(requesterUserId, beforeSnapshot, afterSnapshot);
        topHoldingStockCacheRepository.evictByUserId(requesterUserId);
    }

    @Override
    @Transactional
    public void unregisterAsset(Long portfolioId, PortfolioGroupDto.UnregisterAssetRequest request, UUID requesterUserId) {
        HoldingMetricsSnapshot beforeSnapshot = getHoldingMetricsSnapshot(requesterUserId);

        PortfolioGroup foundPortfolioGroup = findPortfolioGroupWithAssets(portfolioId);

        Money totalPrice = Money.of(request.getStockPrice(), request.getCurrency());

        foundPortfolioGroup.unregister(
                request.getStockId(),
                request.getAmount(),
                totalPrice,
                requesterUserId
        );

        HoldingMetricsSnapshot afterSnapshot = getHoldingMetricsSnapshot(requesterUserId);
        publishHoldingMetricsIfChanged(requesterUserId, beforeSnapshot, afterSnapshot);
        topHoldingStockCacheRepository.evictByUserId(requesterUserId);
    }

    @Override
    @Transactional
    public void createPortfolioGroup(PortfolioGroupDto.CreatePortfolioGroupRequest request, UUID requesterUserId) {
        PortfolioGroup toSave = PortfolioGroup.create(
                request.getName(),
                requesterUserId,
                request.getIconCode()
        );
        portfolioGroupRepository.save(toSave);
    }

    @Override
    @Transactional
    public void updatePortfolioGroup(Long portfolioGroupId, PortfolioGroupDto.UpdatePortfolioGroupRequest request, UUID requesterUserId) {
        PortfolioGroup existing = findPortfolioGroupWithAssets(portfolioGroupId);

        existing.patch(
                request.getName(),
                request.getIconCode()
        );
    }

    @Override
    @Transactional
    public void deletePortfolioGroup(Long portfolioGroupId, UUID requesterUserId) {
        PortfolioGroup existing = findPortfolioGroupWithAssets(portfolioGroupId);

        existing.ensureDeletable(requesterUserId);

        PortfolioGroup defaultGroup = findDefaultPortfolioGroup(requesterUserId);

        existing.transferAssetsTo(defaultGroup);

        portfolioGroupRepository.delete(existing);
    }

    @Override
    @Transactional
    public void createDefaultPortfolioGroup(UUID targetUserId) {
        PortfolioGroup toSave = PortfolioGroup.createDefault(targetUserId);

        if (portfolioGroupRepository.existDefaultByUserId(targetUserId)) {
            throw new DomainException(AssetErrorCode.DEFAULT_PORTFOLIO_GROUP_ALREADY_EXISTS);
        }

        portfolioGroupRepository.save(toSave);
    }

    private PortfolioGroup findPortfolioGroupWithAssets(Long id) {
        return portfolioGroupRepository.findByIdWithAssets(id)
                .orElseThrow(() -> new DomainException(AssetErrorCode.PORTFOLIO_GROUP_NOT_FOUND));
    }

    private PortfolioGroup findDefaultPortfolioGroup(UUID userId) {
        return portfolioGroupRepository.findDefaultByUserId(userId)
                .orElseThrow(() -> new DomainException(AssetErrorCode.DEFAULT_PORTFOLIO_GROUP_NOT_FOUND));
    }

    private Asset toAssetEntity(PortfolioGroupDto.RegisterAssetRequest request, UUID requesterUserId) {
        return Asset.create(
                request.getAmount(),
                request.getStockPrice(),
                request.getCurrency(),
                request.getName(),
                request.getStockId(),
                requesterUserId
        );
    }

    private HoldingMetricsSnapshot getHoldingMetricsSnapshot(UUID userId) {
        List<PortfolioGroup> portfolios = portfolioGroupRepository.findAllByUserIdWithAssets(userId);
        int holdingStockCount = (int) portfolios.stream()
                .flatMap(portfolio -> portfolio.getAssets().stream())
                .map(Asset::getStockId)
                .distinct()
                .count();
        int portfolioWithStocksCount = (int) portfolios.stream()
                .filter(portfolio -> portfolio.getAssets() != null && !portfolio.getAssets().isEmpty())
                .count();
        return new HoldingMetricsSnapshot(holdingStockCount, portfolioWithStocksCount);
    }

    private void publishHoldingMetricsIfChanged(
            UUID userId,
            HoldingMetricsSnapshot beforeSnapshot,
            HoldingMetricsSnapshot afterSnapshot
    ) {
        if (beforeSnapshot.holdingStockCount() != afterSnapshot.holdingStockCount()) {
            gamificationEventProducer.publishUserMetricUpdatedEvent(UserMetricUpdatedEvent.builder()
                    .userId(userId.toString())
                    .eventType(MetricEventType.HOLDING_STOCK_COUNT_CHANGED)
                    .delta((double) afterSnapshot.holdingStockCount())
                    .occurredAt(Instant.now())
                    .build());
        }

        if (beforeSnapshot.portfolioWithStocksCount() != afterSnapshot.portfolioWithStocksCount()) {
            gamificationEventProducer.publishUserMetricUpdatedEvent(UserMetricUpdatedEvent.builder()
                    .userId(userId.toString())
                    .eventType(MetricEventType.PORTFOLIO_WITH_STOCKS_COUNT_CHANGED)
                    .delta((double) afterSnapshot.portfolioWithStocksCount())
                    .occurredAt(Instant.now())
                    .build());
        }

    }

    private record HoldingMetricsSnapshot(int holdingStockCount, int portfolioWithStocksCount) {
    }


    private static class HoldingSummary {
        private final String name;
        private BigDecimal totalAmount;

        private HoldingSummary(String name, BigDecimal totalAmount) {
            this.name = name;
            this.totalAmount = totalAmount;
        }

        private String name() {
            return name;
        }

        private BigDecimal totalAmount() {
            return totalAmount;
        }

        private void addAmount(BigDecimal amount) {
            this.totalAmount = this.totalAmount.add(amount);
        }
    }
}
