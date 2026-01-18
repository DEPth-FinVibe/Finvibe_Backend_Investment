package depth.finvibe.investment.modules.market.application;

import depth.finvibe.investment.modules.market.application.port.in.CurrentPriceCommandUseCase;
import depth.finvibe.investment.shared.dto.TradeExecutedEvent;
import depth.finvibe.investment.shared.dto.UserLoginedEvent;
import depth.finvibe.investment.shared.dto.UserLogoutedEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketEventService {

    private final CurrentPriceCommandUseCase currentPriceCommandUseCase;


    @Transactional
    public void handleTradeExecutedEvent(TradeExecutedEvent event) {
        if (event.getStockId() == null || event.getAmount() == null) {
            log.warn("Skip trade executed event: stockId or amount is missing.");
            return;
        }

        int direction = switch (event.getType()) {
            case "BUY" -> 1;
            case "SELL" -> -1;
            default -> 0;
        };
        if (direction == 0) {
            log.warn("Ignoring trade event of type: {}", event.getType());
            return;
        }

        log.info("Processing trade executed: stockId={}, type={}, amount={}", event.getStockId(), event.getType(), event.getAmount());
        currentPriceCommandUseCase.updateStockHoldingAmount(
                event.getStockId(),
                event.getAmount().multiply(BigDecimal.valueOf(direction))
        );
    }
}
