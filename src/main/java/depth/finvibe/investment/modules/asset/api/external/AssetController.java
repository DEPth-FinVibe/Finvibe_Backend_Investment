package depth.finvibe.investment.modules.asset.api.external;

import depth.finvibe.investment.boot.security.model.AuthenticatedUser;
import depth.finvibe.investment.boot.security.model.Requester;
import depth.finvibe.investment.modules.asset.application.port.in.AssetCommandUseCase;
import depth.finvibe.investment.modules.asset.application.port.in.AssetQueryUseCase;
import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/external/portfolios")
@RequiredArgsConstructor
public class AssetController {

    private final AssetQueryUseCase queryUseCase;
    private final AssetCommandUseCase commandUseCase;

    @GetMapping("/{portfolioId}/assets")
    public ResponseEntity<List<PortfolioGroupDto.AssetResponse>> getAssetsByPortfolio(
            @PathVariable Long portfolioId,
            @AuthenticatedUser Requester requester
    ) {
        return ResponseEntity.ok(queryUseCase.getAssetsByPortfolio(portfolioId, requester.getUuid()));
    }

    @PostMapping("/{portfolioId}/assets")
    public ResponseEntity<Void> registerAsset(
            @PathVariable Long portfolioId,
            @RequestBody @Valid PortfolioGroupDto.RegisterAssetRequest request,
            @AuthenticatedUser Requester requester
    ) {
        commandUseCase.registerAsset(portfolioId, request, requester.getUuid());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{portfolioId}/assets")
    public ResponseEntity<Void> unregisterAsset(
            @PathVariable Long portfolioId,
            @RequestBody @Valid PortfolioGroupDto.UnregisterAssetRequest request,
            @AuthenticatedUser Requester requester
    ) {
        commandUseCase.unregisterAsset(portfolioId, request, requester.getUuid());
        return ResponseEntity.noContent().build();
    }
}
