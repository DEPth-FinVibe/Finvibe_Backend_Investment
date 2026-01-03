package depth.finvibe.investment.modules.asset.application.port.in;

import java.util.UUID;

import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;

public interface PortfolioGroupCommandUseCase {
    void createPortfolioGroup(PortfolioGroupDto.CreatePortfolioGroupRequest request, UUID requesterUserId);
    void updatePortfolioGroup(Long portfolioGroupId, PortfolioGroupDto.UpdatePortfolioGroupRequest request, UUID requesterUserId);
}
