package depth.finvibe.investment.modules.asset.api.external;

import depth.finvibe.investment.boot.security.model.AuthenticatedUser;
import depth.finvibe.investment.boot.security.model.Requester;
import depth.finvibe.investment.modules.asset.application.port.in.AssetQueryUseCase;
import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/portfolios")
@RequiredArgsConstructor
public class AssetController {

    private final AssetQueryUseCase queryUseCase;

    @GetMapping("/{portfolioId}/assets")
    public ResponseEntity<List<PortfolioGroupDto.AssetResponse>> getAssetsByPortfolio(
            @PathVariable Long portfolioId,
            @AuthenticatedUser Requester requester
    ) {
        return ResponseEntity.ok(queryUseCase.getAssetsByPortfolio(portfolioId, requester.getUuid()));
    }
}
