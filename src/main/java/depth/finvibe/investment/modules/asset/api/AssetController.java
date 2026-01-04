package depth.finvibe.investment.modules.asset.api;

import depth.finvibe.investment.modules.asset.application.port.in.AssetCommandUseCase;
import depth.finvibe.investment.modules.asset.application.port.in.AssetQueryUseCase;
import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;
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

    //자산 상태 조회 api
    @GetMapping("/{portfolioId}/assets")
    public ResponseEntity<List<PortfolioGroupDto.AssetResponse>> getAssetsByPortfolio(
            @PathVariable Long portfolioId,
            @RequestParam("userId") UUID userId
    )
    {
        return ResponseEntity.ok(queryUseCase.getAssetsByPortfolio(portfolioId,userId));
    }

    //자산 등록 api
    @PostMapping("/{portfolioId}/assets")
    public ResponseEntity<Void> registerAsset(
            @PathVariable Long portfolioId,
            @RequestBody PortfolioGroupDto.RegisterAssetRequest request,
            @RequestParam("userId") UUID userId
    )
    {
        commandUseCase.registerAsset(portfolioId,request,userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    //자산 삭제 api
    @DeleteMapping("/{portfolioId}/assets")
    public ResponseEntity<Void> unregisterAsset(
            @PathVariable Long portfolioId,
            @RequestBody PortfolioGroupDto.UnregisterAssetRequest request,
            @RequestParam("userId") UUID userId
    )
    {
        commandUseCase.unregisterAsset(portfolioId,request,userId);
        return ResponseEntity.noContent().build();
    }
}
