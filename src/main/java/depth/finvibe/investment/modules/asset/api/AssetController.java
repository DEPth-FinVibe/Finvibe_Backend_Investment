package depth.finvibe.investment.modules.asset.api;

import depth.finvibe.investment.modules.asset.application.port.in.AssetQueryUseCase;
import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/asset")
@RequiredArgsConstructor
public class AssetController {

    private final AssetQueryUseCase queryUseCase;

    @GetMapping("/portfolio")
    public List<PortfolioGroupDto.AssetResponse> getAssetsByPortfolio(
            @RequestParam("portfolioId") Long portfolioId,
            @RequestParam("userId") String userId
    )
    {
        UUID userUuId = UUID.fromString(userId);
        return queryUseCase.getAssetsByPortfolio(portfolioId,userUuId);
    }

    @GetMapping("/user")
    public List<PortfolioGroupDto.PortfolioGroupResponse> getPortfoliosByUser(
            @RequestParam("userId") String userId
    )
    {
        UUID userUuId = UUID.fromString(userId);
        return queryUseCase.getPortfoliosByUser(userUuId);
    }
}