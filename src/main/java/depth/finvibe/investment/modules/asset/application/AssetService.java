package depth.finvibe.investment.modules.asset.application;

import depth.finvibe.investment.modules.asset.application.port.in.AssetQueryUseCase;
import depth.finvibe.investment.modules.asset.application.port.out.PortfolioGroupRepository;
import depth.finvibe.investment.modules.asset.domain.Asset;
import depth.finvibe.investment.modules.asset.domain.Money;
import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;
import depth.finvibe.investment.modules.asset.domain.error.AssetErrorCode;
import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;
import depth.finvibe.investment.shared.error.DomainException;
import org.springframework.stereotype.Service;

import depth.finvibe.investment.modules.asset.application.port.in.AssetCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetService implements AssetCommandUseCase, AssetQueryUseCase {
    private final PortfolioGroupRepository portfolioGroupRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioGroupDto.AssetResponse> getAssetsByPortfolio(Long portfolioId, UUID requesterUserId) {
        PortfolioGroup portfolioGroup = portfolioGroupRepository.findByIdWithAssets(portfolioId)
                .orElseThrow(() -> new DomainException(AssetErrorCode.PORTFOLIO_GROUP_NOT_FOUND));

        if (!portfolioGroup.getUserId().equals(requesterUserId)) {
            throw new DomainException(AssetErrorCode.ONLY_OWNER_CAN_VIEW_ASSETS);
        }

        return portfolioGroup.getAssets().stream()
                .map(asset -> PortfolioGroupDto.AssetResponse.builder()
                        .id(asset.getId())
                        .name(asset.getName())
                        .amount(asset.getAmount())
                        .totalPrice(asset.getTotalPrice().getAmount())
                        .currency(asset.getTotalPrice().getCurrency())
                        .stockId(asset.getStockId())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioGroupDto.PortfolioGroupResponse> getPortfoliosByUser(UUID userId) {
        return portfolioGroupRepository.findAllByUserId(userId).stream()
                .map(group -> PortfolioGroupDto.PortfolioGroupResponse.builder()
                        .id(group.getId())
                        .name(group.getName())
                        .iconCode(group.getIconCode())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void registerAsset(Long portfolioId, PortfolioGroupDto.RegisterAssetRequest request, UUID requesterUserId) {
        PortfolioGroup foundPortfolioGroup = portfolioGroupRepository.findByIdWithAssets(portfolioId)
                .orElseThrow(() -> new DomainException(AssetErrorCode.PORTFOLIO_GROUP_NOT_FOUND));

        Asset toRegister = Asset.create(
                request.getAmount(),
                Money.of(request.getStockPrice().multiply(request.getAmount()), request.getCurrency()),
                request.getName(),
                request.getStockId(),
                requesterUserId
        );
        foundPortfolioGroup.register(toRegister, requesterUserId);
    }

    @Override
    @Transactional
    public void unregisterAsset(Long portfolioId, PortfolioGroupDto.UnregisterAssetRequest request, UUID requesterUserId) {
        PortfolioGroup foundPortfolioGroup = portfolioGroupRepository.findByIdWithAssets(portfolioId)
                .orElseThrow(() -> new DomainException(AssetErrorCode.PORTFOLIO_GROUP_NOT_FOUND));

        foundPortfolioGroup.unregister(
                request.getStockId(),
                request.getAmount(),
                Money.of(request.getStockPrice(), request.getCurrency()),
                requesterUserId
        );
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
        PortfolioGroup existing = portfolioGroupRepository.findById(portfolioGroupId)
                .orElseThrow(() -> new DomainException(AssetErrorCode.PORTFOLIO_GROUP_NOT_FOUND));

        existing.patch(
                request.getName(),
                request.getIconCode()
        );
    }

    @Override
    @Transactional
    public void deletePortfolioGroup(Long portfolioGroupId, UUID requesterUserId) {
        PortfolioGroup existing = portfolioGroupRepository.findById(portfolioGroupId)
                .orElseThrow(() -> new DomainException(AssetErrorCode.PORTFOLIO_GROUP_NOT_FOUND));

        existing.ensureDeletable(requesterUserId);

        PortfolioGroup defaultGroup = portfolioGroupRepository.findDefaultByUserId(requesterUserId)
                .orElseThrow(() -> new DomainException(AssetErrorCode.DEFAULT_PORTFOLIO_GROUP_NOT_FOUND));

        existing.transferAssetsTo(defaultGroup);

        portfolioGroupRepository.delete(existing);
    }
}
