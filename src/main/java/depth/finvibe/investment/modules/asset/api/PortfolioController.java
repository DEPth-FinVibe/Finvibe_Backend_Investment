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
public class PortfolioController {
    private final AssetCommandUseCase commandUseCase;
    private final AssetQueryUseCase queryUseCase;

    //포트폴리오 조회 api
    @GetMapping("")
    public ResponseEntity<List<PortfolioGroupDto.PortfolioGroupResponse>> getPortfoliosByUser(
            @RequestParam UUID userId
    )
    {
        return ResponseEntity.ok(queryUseCase.getPortfoliosByUser(userId));
    }

    //포트폴리오 생성 api
    @PostMapping
    public ResponseEntity<Void> createPortfolioGroup(
            @RequestBody PortfolioGroupDto.CreatePortfolioGroupRequest request,
            @RequestParam UUID userId
    )
    {
        commandUseCase.createPortfolioGroup(request,userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    //포트폴리오 업데이트 api
    @PatchMapping("/{portfolioGroupId}")
    public ResponseEntity<Void> updatePortfolioGroup(
            @PathVariable Long portfolioGroupId,
            @RequestBody PortfolioGroupDto.UpdatePortfolioGroupRequest request,
            @RequestParam UUID userId
    )
    {
        commandUseCase.updatePortfolioGroup(portfolioGroupId,request,userId);
        return ResponseEntity.ok().build();
    }

    //포트폴리오 삭제 api
    @DeleteMapping("/{portfolioGroupId}")
    public ResponseEntity<Void> deletePortfolioGroup(
            @PathVariable Long portfolioGroupId,
            @RequestParam UUID userId
    )
    {
        commandUseCase.deletePortfolioGroup(portfolioGroupId, userId);
        return ResponseEntity.noContent().build();
    }

    //기본 포트폴리오 생성
    @PostMapping("/default")
    public ResponseEntity<Void> createDefaultPortfolioGroup(
            @RequestParam UUID userId
    )
    {
        commandUseCase.createDefaultPortfolioGroup(userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
