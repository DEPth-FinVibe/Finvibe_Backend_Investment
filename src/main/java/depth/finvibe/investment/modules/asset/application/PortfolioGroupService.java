package depth.finvibe.investment.modules.asset.application;

import depth.finvibe.investment.modules.asset.application.port.out.PortfolioGroupRepository;
import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;
import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;
import org.springframework.stereotype.Service;

import depth.finvibe.investment.modules.asset.application.port.in.PortfolioGroupCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortfolioGroupService implements PortfolioGroupCommandUseCase{
    private final PortfolioGroupRepository portfolioGroupRepository;

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
}
