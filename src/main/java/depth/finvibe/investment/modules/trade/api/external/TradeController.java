package depth.finvibe.investment.modules.trade.api.external;

import jakarta.validation.Valid;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import depth.finvibe.investment.boot.security.model.AuthenticatedUser;
import depth.finvibe.investment.boot.security.model.Requester;
import depth.finvibe.investment.modules.trade.application.port.in.TradeCommandUseCase;
import depth.finvibe.investment.modules.trade.application.port.in.TradeQueryUseCase;
import depth.finvibe.investment.modules.trade.dto.TradeDto;

@RestController
@RequestMapping("/trades")
@RequiredArgsConstructor
@Tag(name = "거래", description = "거래 API")
public class TradeController {
    private final TradeCommandUseCase tradeCommandUseCase;
    private final TradeQueryUseCase tradeQueryUseCase;

    @GetMapping("/{tradeId}")
    @Operation(summary = "거래 조회", description = "거래 ID로 거래 상태를 조회합니다.")
    public ResponseEntity<TradeDto.TradeResponse> getTradeStatus(
            @Parameter(description = "거래 ID", example = "123") @PathVariable Long tradeId
    ) {
        TradeDto.TradeResponse response = tradeQueryUseCase.findTrade(tradeId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reserved/stock-ids")
    @Operation(summary = "예약 종목 ID 조회", description = "사용자의 예약 종목 ID 목록을 조회합니다.")
    public ResponseEntity<List<Long>> getReservedStockIds(
            @Parameter(hidden = true) @AuthenticatedUser Requester requester
    ) {
        List<Long> stockIds = tradeQueryUseCase.findReservedStockIds(requester.getUuid());
        return ResponseEntity.ok(stockIds);
    }

    @PostMapping
    @Operation(summary = "거래 생성", description = "신규 거래 주문을 생성합니다.")
    public ResponseEntity<TradeDto.TradeResponse> placeTrade(
            @RequestBody @Valid TradeDto.TransactionRequest request
    ) {
        TradeDto.TradeResponse response = tradeCommandUseCase.createTrade(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{tradeId}")
    @Operation(summary = "거래 취소", description = "거래 주문을 취소합니다.")
    public ResponseEntity<TradeDto.TradeResponse> cancelTrade(
            @Parameter(description = "거래 ID", example = "123") @PathVariable Long tradeId
    ) {
        TradeDto.TradeResponse response = tradeCommandUseCase.cancelTrade(tradeId);
        return ResponseEntity.ok(response);
    }
}
