package depth.finvibe.investment.modules.trade.api.internal;

import depth.finvibe.investment.modules.trade.application.port.in.TradeQueryUseCase;
import depth.finvibe.investment.modules.trade.dto.TradeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/trades")
@RequiredArgsConstructor
public class TradeInternalController {
    private final TradeQueryUseCase tradeQueryUseCase;

    @GetMapping("/{tradeId}")
    public ResponseEntity<TradeDto.TradeResponse> getTradeStatus(
            @PathVariable Long tradeId
    ) {
        TradeDto.TradeResponse response = tradeQueryUseCase.findTrade(tradeId);
        return ResponseEntity.ok(response);
    }
}
