package depth.finvibe.investment.modules.asset.application.port.in;

import java.util.UUID;

import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;

public interface AssetCommandUseCase {
    void registerAsset(Long portfolioId, PortfolioGroupDto.RegisterAssetRequest request, UUID requesterUserId);
    void unregisterAsset(Long portfolioId, PortfolioGroupDto.UnregisterAssetRequest request, UUID requesterUserId);
    void createPortfolioGroup(PortfolioGroupDto.CreatePortfolioGroupRequest request, UUID requesterUserId);
    void updatePortfolioGroup(Long portfolioGroupId, PortfolioGroupDto.UpdatePortfolioGroupRequest request, UUID requesterUserId);
    void deletePortfolioGroup(Long portfolioGroupId, UUID requesterUserId);
    void createDefaultPortfolioGroup(UUID targetUserId);
}
