package depth.finvibe.investment.modules.market.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import depth.finvibe.investment.modules.market.application.port.out.BatchUpdatePriceRepository;
import depth.finvibe.investment.modules.market.application.port.out.HoldingStockRepository;
import depth.finvibe.investment.modules.market.application.port.out.RealMarketClient;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.BatchUpdatePrice;
import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.dto.PriceCandleDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchPriceUpdateService {

  private final HoldingStockRepository holdingStockRepository;
  private final StockRepository stockRepository;
  private final RealMarketClient realMarketClient;
  private final BatchUpdatePriceRepository batchUpdatePriceRepository;

  public void updateHoldingStockPrices() {
    log.info("Starting batch price update for holding stocks");

    // 1. 보유중인 모든 주식 ID 조회
    List<Long> holdingStockIds = holdingStockRepository.findAllDistinctStockIds();
    if (holdingStockIds.isEmpty()) {
      log.info("No holding stocks found. Skipping batch price update.");
      return;
    }

    log.info("Found {} distinct holding stocks", holdingStockIds.size());

    // 2. Stock 엔티티에서 symbol 매핑 (stockId -> symbol)
    List<Stock> stocks = stockRepository.findAllById(holdingStockIds);
    Map<Long, String> stockIdToSymbolMap = stocks.stream()
            .collect(Collectors.toMap(Stock::getId, Stock::getSymbol));

    List<String> symbols = stocks.stream()
            .map(Stock::getSymbol)
            .toList();

    if (symbols.isEmpty()) {
      log.warn("No valid stock symbols found for holding stocks");
      return;
    }

    // 3. bulkFetchCurrentPrices 호출
    try {
      List<PriceCandleDto.Response> priceResponses = realMarketClient.bulkFetchCurrentPrices(symbols);
      
      if (priceResponses.isEmpty()) {
        log.warn("No price data returned from market client");
        return;
      }

      log.info("Fetched {} price updates from market client", priceResponses.size());

      // 4. BatchUpdatePrice 도메인 모델로 변환
      List<BatchUpdatePrice> batchPrices = priceResponses.stream()
              .map(BatchUpdatePrice::from)
              .toList();

      // 5. Redis에 일괄 저장
      batchUpdatePriceRepository.saveAll(batchPrices);

      log.info("Successfully saved {} batch price updates to Redis", batchPrices.size());
    } catch (Exception e) {
      log.error("Failed to update batch prices for holding stocks", e);
      throw e;
    }
  }
}
