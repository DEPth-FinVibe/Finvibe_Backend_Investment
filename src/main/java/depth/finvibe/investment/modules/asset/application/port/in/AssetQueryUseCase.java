package depth.finvibe.investment.modules.asset.application.port.in;

import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;

import java.util.List;
import java.util.UUID;

public interface AssetQueryUseCase {
    List<PortfolioGroupDto.AssetResponse> getAssetsByPortfolio(Long portfolioId, UUID requesterUserId);
    List<PortfolioGroupDto.PortfolioGroupResponse> getPortfoliosByUser(UUID userId);
}
