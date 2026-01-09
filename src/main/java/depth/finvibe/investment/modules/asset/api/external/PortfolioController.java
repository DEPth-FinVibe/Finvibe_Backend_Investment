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
public class PortfolioController {
    private final AssetCommandUseCase commandUseCase;
    private final AssetQueryUseCase queryUseCase;

    @GetMapping
    public ResponseEntity<List<PortfolioGroupDto.PortfolioGroupResponse>> getPortfoliosByUser(
            @AuthenticatedUser Requester requester
    ) {
        return ResponseEntity.ok(queryUseCase.getPortfoliosByUser(requester.getUuid()));
    }

    @PostMapping
    public ResponseEntity<Void> createPortfolioGroup(
            @RequestBody @Valid PortfolioGroupDto.CreatePortfolioGroupRequest request,
            @AuthenticatedUser Requester requester
    ) {
        commandUseCase.createPortfolioGroup(request, requester.getUuid());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{portfolioGroupId}")
    public ResponseEntity<Void> updatePortfolioGroup(
            @PathVariable Long portfolioGroupId,
            @RequestBody @Valid PortfolioGroupDto.UpdatePortfolioGroupRequest request,
            @AuthenticatedUser Requester requester
    ) {
        commandUseCase.updatePortfolioGroup(portfolioGroupId, request, requester.getUuid());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{portfolioGroupId}")
    public ResponseEntity<Void> deletePortfolioGroup(
            @PathVariable Long portfolioGroupId,
            @AuthenticatedUser Requester requester
    ) {
        commandUseCase.deletePortfolioGroup(portfolioGroupId, requester.getUuid());
        return ResponseEntity.noContent().build();
    }
}
