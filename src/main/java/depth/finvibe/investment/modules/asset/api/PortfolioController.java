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
public class PortfolioController {
    private final AssetCommandUseCase commandUseCase;
    private final AssetQueryUseCase queryUseCase;

    @GetMapping
    public ResponseEntity<List<PortfolioGroupDto.PortfolioGroupResponse>> getPortfoliosByUser(
            @RequestParam UUID userId
    )
    {
        return ResponseEntity.ok(queryUseCase.getPortfoliosByUser(userId));
    }

    @PostMapping
    public ResponseEntity<Void> createPortfolioGroup(
            @RequestBody @Valid PortfolioGroupDto.CreatePortfolioGroupRequest request,
            @RequestParam UUID userId
    )
    {
        commandUseCase.createPortfolioGroup(request,userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{portfolioGroupId}")
    public ResponseEntity<Void> updatePortfolioGroup(
            @PathVariable Long portfolioGroupId,
            @RequestBody @Valid PortfolioGroupDto.UpdatePortfolioGroupRequest request,
            @RequestParam UUID userId
    )
    {
        commandUseCase.updatePortfolioGroup(portfolioGroupId,request,userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{portfolioGroupId}")
    public ResponseEntity<Void> deletePortfolioGroup(
            @PathVariable Long portfolioGroupId,
            @RequestParam UUID userId
    )
    {
        commandUseCase.deletePortfolioGroup(portfolioGroupId, userId);
        return ResponseEntity.noContent().build();
    }
}
