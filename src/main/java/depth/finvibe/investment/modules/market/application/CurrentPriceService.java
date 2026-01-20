package depth.finvibe.investment.modules.market.application;

import depth.finvibe.investment.modules.market.application.port.in.CurrentPriceCommandUseCase;
import depth.finvibe.investment.modules.market.application.port.out.CurrentPriceEventPublisher;
import depth.finvibe.investment.modules.market.application.port.out.CurrentPriceRepository;
import depth.finvibe.investment.modules.market.application.port.out.HoldingStockRepository;
import depth.finvibe.investment.modules.market.application.port.out.RealtimeStockIndexRepository;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.CurrentPrice;
import depth.finvibe.investment.modules.market.domain.RealtimeStockIndex;
import depth.finvibe.investment.modules.market.domain.error.MarketErrorCode;
import depth.finvibe.investment.modules.market.dto.CurrentPriceUpdatedEvent;
import depth.finvibe.investment.shared.error.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrentPriceService implements CurrentPriceCommandUseCase {

    private final StockRepository stockRepository;
    private final HoldingStockRepository holdingStockRepository;
    private final RealtimeStockIndexRepository realtimeStockIndexRepository;
    private final CurrentPriceRepository currentPriceRepository;
    private final CurrentPriceEventPublisher currentPriceEventPublisher;

    @Override
    public void registerWatchingStock(Long stockId, UUID userId) {
        checkStockIsExist(stockId);

        realtimeStockIndexRepository.addRealtimeStockIndex(RealtimeStockIndex.create(stockId, userId));
    }

    @Override
    public void unregisterWatchingStock(Long stockId, UUID userId) {
        checkStockIsExist(stockId);

        realtimeStockIndexRepository.removeRealtimeStockIndex(RealtimeStockIndex.create(stockId, userId));
    }

    @Override
    public void registerHoldingStock(Long stockId, UUID userId) {
        checkStockIsExist(stockId);

        holdingStockRepository.registerHoldingStock(stockId, userId);
    }

    @Override
    public void unregisterHoldingStock(Long stockId, UUID userId) {
        checkStockIsExist(stockId);

        holdingStockRepository.unregisterHoldingStock(stockId, userId);
    }

    @Override
    public void stockPriceUpdated(CurrentPriceUpdatedEvent priceUpdate) {
        if(!realtimeStockIndexRepository.existsByStockId(priceUpdate.getStockId())) {
            log.warn("Skipping stock price update for stockId={} as it is not in the realtime index.", priceUpdate.getStockId());
            return;
        }

        currentPriceRepository.upsertCurrentPrice(CurrentPrice.from(priceUpdate));
        currentPriceEventPublisher.publish(priceUpdate);
    }


    private void checkStockIsExist(Long stockId) {
        if(!stockRepository.existsById(stockId)) {
            throw new DomainException(MarketErrorCode.STOCK_NOT_FOUND);
        }
    }


}
