package depth.finvibe.investment.modules.trade.api;

import depth.finvibe.investment.modules.trade.application.port.in.TradeCommandUseCase;
import depth.finvibe.investment.modules.trade.application.port.in.TradeQueryUseCase;
import depth.finvibe.investment.modules.trade.dto.TradeDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trades")
@RequiredArgsConstructor
public class TradeController {
    private final TradeCommandUseCase tradeCommandUseCase;
    private final TradeQueryUseCase tradeQueryUseCase;

    @GetMapping("/{tradeId}")
    public ResponseEntity<TradeDto.TradeResponse> getTradeStatus(
            @PathVariable Long tradeId
    ) {
        TradeDto.TradeResponse response = tradeQueryUseCase.findTrade(tradeId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<TradeDto.TradeResponse> placeTrade(
            @RequestBody @Valid TradeDto.TransactionRequest request
    ) {
        TradeDto.TradeResponse response = tradeCommandUseCase.createTrade(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{tradeId}")
    public ResponseEntity<TradeDto.TradeResponse> cancelTrade(
            @PathVariable Long tradeId
    ) {
        TradeDto.TradeResponse response = tradeCommandUseCase.cancelTrade(tradeId);
        return ResponseEntity.ok(response);
    }
}
