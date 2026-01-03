package depth.finvibe.investment.modules.asset.application;

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

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetService implements AssetCommandUseCase {
    private final PortfolioGroupRepository portfolioGroupRepository;

    @Override
    public void registerAsset(Long portfolioId, PortfolioGroupDto.RegisterAssetRequest request, UUID requesterUserId) {
        PortfolioGroup foundPortfolioGroup = portfolioGroupRepository.findById(portfolioId)
                .orElseThrow(() -> new DomainException(AssetErrorCode.PORTFOLIO_GROUP_NOT_FOUND));

        Asset toRegister = Asset.create(
                request.getAmount(),
                Money.of(request.getPrice() * request.getAmount(), request.getCurrency()),
                request.getName(),
                request.getStockId(),
                requesterUserId
        );
        foundPortfolioGroup.register(toRegister, requesterUserId);
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
}
