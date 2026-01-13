package depth.finvibe.investment.modules.market.application;

import depth.finvibe.investment.modules.market.application.port.in.MarketCommandUseCase;
import depth.finvibe.investment.shared.dto.UserLoginedEvent;
import depth.finvibe.investment.shared.dto.UserLogoutedEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.HashSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketEventService {

    private final MarketCommandUseCase marketCommandUseCase;

    @Transactional
    public void handleUserLogin(UserLoginedEvent event) {
        log.info("Processing user login: userId={}, interestedStockIds={}, ownedStockIds = {} ", event.userId(), event.interestedStockIds(), event.ownedStockIds());
        marketCommandUseCase.addRegionOfInterestLevel1(event.interestedStockIds());
        marketCommandUseCase.addRegionOfInterestLevel2(event.ownedStockIds());
    }

    @Transactional
    public void handleUserLogout(UserLogoutedEvent event) {
        log.info("Processing user logout: userId={}, interestedStockIds={}, ownedStockIds = {} ", event.userId(), event.interestedStockIds(), event.ownedStockIds());
        marketCommandUseCase.removeRegionOfInterestLevel1(event.interestedStockIds());
        marketCommandUseCase.removeRegionOfInterestLevel2(event.ownedStockIds());
    }
}