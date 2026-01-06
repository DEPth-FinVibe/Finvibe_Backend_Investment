package depth.finvibe.investment.modules.asset.api;

import depth.finvibe.investment.modules.asset.application.port.in.AssetCommandUseCase;
import depth.finvibe.investment.modules.asset.application.port.in.AssetQueryUseCase;
import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/portfolios")
@RequiredArgsConstructor
public class AssetController {

    private final AssetQueryUseCase queryUseCase;
    private final AssetCommandUseCase commandUseCase;

    @GetMapping("/{portfolioId}/assets")
    public ResponseEntity<List<PortfolioGroupDto.AssetResponse>> getAssetsByPortfolio(
            @PathVariable Long portfolioId,
            @RequestParam("userId") UUID userId
    )
    {
        return ResponseEntity.ok(queryUseCase.getAssetsByPortfolio(portfolioId,userId));
    }

    @PostMapping("/{portfolioId}/assets")
    public ResponseEntity<Void> registerAsset(
            @PathVariable Long portfolioId,
            @RequestBody @Valid PortfolioGroupDto.RegisterAssetRequest request,
            @RequestParam("userId") UUID userId
    )
    {
        commandUseCase.registerAsset(portfolioId,request,userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{portfolioId}/assets")
    public ResponseEntity<Void> unregisterAsset(
            @PathVariable Long portfolioId,
            @RequestBody @Valid PortfolioGroupDto.UnregisterAssetRequest request,
            @RequestParam("userId") UUID userId
    )
    {
        commandUseCase.unregisterAsset(portfolioId,request,userId);
        return ResponseEntity.noContent().build();
    }
}
