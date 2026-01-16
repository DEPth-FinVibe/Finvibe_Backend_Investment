package depth.finvibe.investment.modules.market.application;

import depth.finvibe.investment.modules.market.application.port.in.MarketCommandUseCase;
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

    private final MarketCommandUseCase marketCommandUseCase;

    @Transactional
    public void handleUserLogin(UserLoginedEvent event) {
        log.info("Processing user login: userId={}, interestedStockIds={}, ownedStockIds = {} ", event.userId(), event.interestedStockIds(), event.ownedStockIds());
        marketCommandUseCase.addRegionOfInterestLevel1(event.interestedStockIds());
    }

    @Transactional
    public void handleUserLogout(UserLogoutedEvent event) {
        log.info("Processing user logout: userId={}, interestedStockIds={}, ownedStockIds = {} ", event.userId(), event.interestedStockIds(), event.ownedStockIds());
        marketCommandUseCase.removeRegionOfInterestLevel1(event.interestedStockIds());
    }

    @Transactional
    public void handleTradeExecutedEvent(TradeExecutedEvent event) {
        if (event.stockId() == null || event.amount() == null) {
            log.warn("Skip trade executed event: stockId or amount is missing.");
            return;
        }

        int direction = switch (event.type()) {
            case "BUY" -> 1;
            case "SELL" -> -1;
            default -> 0;
        };
        if (direction == 0) {
            log.warn("Ignoring trade event of type: {}", event.type());
            return;
        }

        log.info("Processing trade executed: stockId={}, type={}, amount={}", event.stockId(), event.type(), event.amount());
        marketCommandUseCase.updateStockHoldingAmount(
                event.stockId(),
                BigDecimal.valueOf(event.amount()).multiply(BigDecimal.valueOf(direction))
        );
    }
}
