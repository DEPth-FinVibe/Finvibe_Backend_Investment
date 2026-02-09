package depth.finvibe.investment.modules.asset.application.port.in;

import java.util.List;
import java.util.UUID;

import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;
import depth.finvibe.investment.modules.asset.dto.TopHoldingStockDto;

public interface AssetQueryUseCase {
    List<PortfolioGroupDto.AssetResponse> getAssetsByPortfolio(Long portfolioId, UUID requesterUserId);
    List<PortfolioGroupDto.PortfolioGroupResponse> getPortfoliosByUser(UUID userId);
    List<PortfolioGroupDto.PortfolioComparisonResponse> getPortfolioComparisons(UUID userId);
    boolean isExistPortfolio(Long portfolioId, UUID userId);
    TopHoldingStockDto.TopHoldingStockListResponse getTopHoldingStocks(UUID userId);
    PortfolioGroupDto.AssetAllocationResponse getAssetAllocation(UUID requesterUserId);
}
