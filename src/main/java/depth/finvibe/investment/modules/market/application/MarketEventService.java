package depth.finvibe.investment.modules.market.application;

import depth.finvibe.investment.modules.market.application.port.in.CurrentPriceCommandUseCase;
import depth.finvibe.investment.shared.dto.StockHoldingChangedEvent;
import depth.finvibe.investment.shared.dto.TradeExecutedEvent;
import depth.finvibe.investment.shared.dto.UserLoginedEvent;
import depth.finvibe.investment.shared.dto.UserLogoutedEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketEventService {

    private final CurrentPriceCommandUseCase currentPriceCommandUseCase;


    public void handleStockHoldingChangedEvent(StockHoldingChangedEvent event) {
        Long stockId = event.getStockId();
        UUID userId = event.getUserId();
        Boolean isHolding = event.getIsHolding();

        //TODO: 보유종목 갱신

        if(isHolding) {
            currentPriceCommandUseCase.registerHoldingStock(stockId, userId);
        }else{
            currentPriceCommandUseCase.unregisterHoldingStock(stockId, userId);
        }

        log.info("Handled StockHoldingChangedEvent: stockId={}, userId={}, isHolding={}", stockId, userId, isHolding);
    }
}
