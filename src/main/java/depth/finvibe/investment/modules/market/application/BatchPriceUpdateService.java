package depth.finvibe.investment.modules.market.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import depth.finvibe.investment.modules.market.application.port.out.BatchPriceEventProducer;
import depth.finvibe.investment.modules.market.application.port.out.BatchUpdatePriceRepository;
import depth.finvibe.investment.modules.market.application.port.out.CurrentPriceRepository;
import depth.finvibe.investment.modules.market.application.port.out.CurrentStockWatcherRepository;
import depth.finvibe.investment.modules.market.application.port.out.HoldingStockRepository;
import depth.finvibe.investment.modules.market.application.port.out.RealMarketClient;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.BatchUpdatePrice;
import depth.finvibe.investment.modules.market.domain.CurrentPrice;
import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.dto.PriceCandleDto;
import depth.finvibe.investment.shared.dto.BatchPriceUpdatedEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchPriceUpdateService {

  private final HoldingStockRepository holdingStockRepository;
  private final StockRepository stockRepository;
  private final RealMarketClient realMarketClient;
  private final CurrentPriceRepository currentPriceRepository;
  private final CurrentStockWatcherRepository currentStockWatcherRepository;
  private final BatchUpdatePriceRepository batchUpdatePriceRepository;
  private final BatchPriceEventProducer batchPriceEventProducer;

  public void updateHoldingStockPrices() {
    log.info("Starting batch price update for holding stocks");

    List<Long> holdingStockIds = holdingStockRepository.findAllDistinctStockIds();
    if (holdingStockIds.isEmpty()) {
      log.info("No holding stocks found. Skipping batch price update.");
      return;
    }

    log.info("Found {} distinct holding stocks", holdingStockIds.size());

    List<Long> subscribedStockIds = filterSubscribedStockIds(holdingStockIds);
    List<Long> unsubscribedStockIds = filterUnsubscribedStockIds(holdingStockIds, subscribedStockIds);

    log.info("Subscribed stocks: {}, Unsubscribed stocks: {}",
            subscribedStockIds.size(),
            unsubscribedStockIds.size());

    List<BatchUpdatePrice> batchPrices = new ArrayList<>();

    if (!subscribedStockIds.isEmpty()) {
      List<Long> cacheMissStockIds = appendCachedPrices(batchPrices, subscribedStockIds);
      if (!cacheMissStockIds.isEmpty()) {
        log.warn("Cache miss for {} subscribed stocks", cacheMissStockIds.size());
        unsubscribedStockIds = mergeStockIds(unsubscribedStockIds, cacheMissStockIds);
      }
    }

    if (!unsubscribedStockIds.isEmpty()) {
      bulkFetchPricesFromMarket(batchPrices, unsubscribedStockIds);
    }

    if (batchPrices.isEmpty()) {
      log.warn("No price data available for batch update");
      return;
    }

    try {
      batchUpdatePriceRepository.saveAll(batchPrices);

      log.info("Successfully saved {} batch price updates to Redis", batchPrices.size());

      BatchPriceUpdatedEvent event = BatchPriceUpdatedEvent.builder()
              .batchExecutedAt(LocalDateTime.now())
              .totalStockCount(batchPrices.size())
              .updatedStockIds(batchPrices.stream().map(BatchUpdatePrice::getStockId).toList())
              .build();
      batchPriceEventProducer.publishBatchPriceUpdated(event);
      log.info("Published batch price updated event for {} stocks", batchPrices.size());
    } catch (Exception e) {
      log.error("Failed to update batch prices for holding stocks", e);
      throw e;
    }
  }

  private List<Long> filterSubscribedStockIds(List<Long> holdingStockIds) {
    List<Long> subscribedStockIds = new ArrayList<>();
    for (Long stockId : holdingStockIds) {
      if (currentStockWatcherRepository.existsByStockId(stockId)) {
        subscribedStockIds.add(stockId);
      }
    }
    return subscribedStockIds;
  }

  private List<Long> filterUnsubscribedStockIds(List<Long> holdingStockIds, List<Long> subscribedStockIds) {
    Set<Long> subscribedSet = Set.copyOf(subscribedStockIds);
    List<Long> unsubscribedStockIds = new ArrayList<>();
    for (Long stockId : holdingStockIds) {
      if (!subscribedSet.contains(stockId)) {
        unsubscribedStockIds.add(stockId);
      }
    }
    return unsubscribedStockIds;
  }

  private List<Long> appendCachedPrices(List<BatchUpdatePrice> batchPrices, List<Long> subscribedStockIds) {
    List<CurrentPrice> cachedPrices = currentPriceRepository.findByStockIds(subscribedStockIds);
    Map<Long, CurrentPrice> cachedPriceMap = cachedPrices.stream()
            .collect(Collectors.toMap(CurrentPrice::getStockId, Function.identity()));

    cachedPriceMap.values().forEach(price -> batchPrices.add(BatchUpdatePrice.from(price)));
    log.info("Loaded {} prices from current price cache", cachedPriceMap.size());

    return subscribedStockIds.stream()
            .filter(stockId -> !cachedPriceMap.containsKey(stockId))
            .toList();
  }

  private List<Long> mergeStockIds(List<Long> baseIds, List<Long> newIds) {
    Set<Long> merged = new java.util.LinkedHashSet<>(baseIds);
    merged.addAll(newIds);
    return new ArrayList<>(merged);
  }

  private void bulkFetchPricesFromMarket(List<BatchUpdatePrice> batchPrices, List<Long> stockIds) {
    List<String> symbols = fetchSymbols(stockIds);
    if (symbols.isEmpty()) {
      log.warn("No valid stock symbols found for unsubscribed stocks");
      return;
    }

    try {
      List<PriceCandleDto.Response> priceResponses = realMarketClient.bulkFetchCurrentPrices(symbols);

      if (priceResponses.isEmpty()) {
        log.warn("No price data returned from market client");
        return;
      }

      log.info("Fetched {} price updates from market client", priceResponses.size());

      List<BatchUpdatePrice> apiBatchPrices = priceResponses.stream()
              .map(BatchUpdatePrice::from)
              .toList();

      batchPrices.addAll(apiBatchPrices);
    } catch (Exception e) {
      log.error("Failed to update batch prices for holding stocks", e);
      throw e;
    }
  }

  private List<String> fetchSymbols(List<Long> stockIds) {
    List<Stock> stocks = stockRepository.findAllById(stockIds);
    Map<Long, String> stockIdToSymbolMap = stocks.stream()
            .collect(Collectors.toMap(Stock::getId, Stock::getSymbol));

    return stockIds.stream()
            .map(stockIdToSymbolMap::get)
            .filter(symbol -> symbol != null && !symbol.isBlank())
            .toList();
  }
}
