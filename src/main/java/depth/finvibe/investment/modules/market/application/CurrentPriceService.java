package depth.finvibe.investment.modules.market.application;

import depth.finvibe.investment.modules.market.application.port.in.CurrentPriceCommandUseCase;
import depth.finvibe.investment.modules.market.application.port.in.MarketQueryUseCase;
import depth.finvibe.investment.modules.market.application.port.out.*;

import depth.finvibe.investment.modules.market.domain.CurrentPrice;
import depth.finvibe.investment.modules.market.domain.PriceCandle;
import depth.finvibe.investment.modules.market.domain.RealtimeStockIndex;
import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import depth.finvibe.investment.modules.market.domain.error.MarketErrorCode;
import depth.finvibe.investment.modules.market.dto.CurrentPriceDto;
import depth.finvibe.investment.modules.market.dto.CurrentPriceUpdatedEvent;
import depth.finvibe.investment.modules.market.dto.PriceCandleDto;
import depth.finvibe.investment.modules.market.dto.StockDto;
import depth.finvibe.investment.shared.error.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrentPriceService implements MarketQueryUseCase, CurrentPriceCommandUseCase {

    private final PriceCandleRepository priceCandleRepository;
    private final StockRepository stockRepository;
    private final RealMarketClient realMarketClient;
    private final HoldingStockRepository holdingStockRepository;
    private final RealtimeStockIndexRepository realtimeStockIndexRepository;
    private final CurrentPriceRepository currentPriceRepository;

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
    }

    @Override
    @Transactional
    public List<PriceCandleDto.Response> getStockCandles(Long stockId, LocalDateTime startTime, Timeframe timeframe, Integer count) {

        List<PriceCandle> existingCandles = priceCandleRepository.findExisting(stockId, startTime, timeframe, count);

        List<LocalDateTime> missingCandleTimes = calculateMissingCandleTimes(startTime, timeframe, count, existingCandles);
        List<PriceCandleDto.Response> fetchedCandles = realMarketClient.fetchPriceCandles(stockId, missingCandleTimes, timeframe);

        saveNewlyFetchedCandles(fetchedCandles, stockId, timeframe);

        return mergeAndSortCandles(existingCandles, fetchedCandles);
    }

    private List<PriceCandleDto.Response> mergeAndSortCandles(List<PriceCandle> existingCandles, List<PriceCandleDto.Response> fetchedCandles) {
        List<PriceCandleDto.Response> existingCandleDtos = existingCandles.stream()
            .map(PriceCandleDto.Response::from)
            .toList();

        return Stream.concat(existingCandleDtos.stream(), fetchedCandles.stream())
            .sorted(Comparator.comparing(PriceCandleDto.Response::getAt))
            .toList();
    }

    private void saveNewlyFetchedCandles(List<PriceCandleDto.Response> fetchedCandles, Long stockId, Timeframe timeframe) {
        List<PriceCandle> fetchedResult = fetchedCandles.stream()
            .map(this::createPriceCandleFrom)
            .toList();

        priceCandleRepository.saveAll(fetchedResult);
    }

    private PriceCandle createPriceCandleFrom(PriceCandleDto.Response dto) {
        return PriceCandle.builder()
            .stockId(dto.getStockId())
            .timeframe(dto.getTimeframe())
            .at(dto.getAt())
            .open(dto.getOpen())
            .close(dto.getClose())
            .high(dto.getHigh())
            .low(dto.getLow())
            .volume(dto.getVolume())
            .value(dto.getValue())
            .prevDayChangePct(dto.getPrevDayChangePct())
            .build();
    }

    private List<LocalDateTime> calculateMissingCandleTimes(LocalDateTime startTime, Timeframe timeframe, Integer count, List<PriceCandle> existingCandles) {
        Set<LocalDateTime> shouldHaveCandleTimes = generateCandleTimes(startTime, timeframe, count);

        Set<LocalDateTime> existingCandleTimes = existingCandles.stream()
                .map(PriceCandle::getAt)
                .collect(Collectors.toSet());

        Set<LocalDateTime> missingCandleTimes = new HashSet<>(shouldHaveCandleTimes);
        missingCandleTimes.removeAll(existingCandleTimes);

        return missingCandleTimes.stream().toList();
    }

    private Set<LocalDateTime> generateCandleTimes(LocalDateTime startTime, Timeframe timeframe, Integer count) {
        Set<LocalDateTime> candleTimes = new HashSet<>();

        switch (timeframe) {
            case Timeframe.DAY:
                for (int i = 0; i < count; i++) {
                    candleTimes.add(startTime.plusDays(i).withHour(0).withMinute(0).withSecond(0).withNano(0));
                }
                break;
            case Timeframe.HOUR:
                for (int i = 0; i < count; i++) {
                    candleTimes.add(startTime.plusHours(i).withMinute(0).withSecond(0).withNano(0));
                }
                break;
            case Timeframe.MINUTE:
                for (int i = 0; i < count; i++) {
                    candleTimes.add(startTime.plusMinutes(i).withSecond(0).withNano(0));
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
        }

        return candleTimes;
    }



    /***
     * 여러 종목의 현재가를 조회
     * 종목이 인덱스에는 들어있지만 아직 현재가가 캐싱되지 않은 경우 예외 발생됨. Infra에서 시간을 두고 N번 재시도.
     * @param stockIds 조회할 종목 ID 리스트 (현재가 캐시에 존재하는 종목이어야 함)
     * @return 현재가 DTO 리스트
     */
    @Override
    public List<CurrentPriceDto.Response> getCurrentPrices(List<Long> stockIds) {
        if(!realtimeStockIndexRepository.allExistsByStockIds(stockIds)) {
            throw new DomainException(MarketErrorCode.STOCK_NOT_FOUND);
        }

        //인덱스에는 들어왔지만 실제로 값이 들어오지 않은 경우 예외가 발생. Infra에서 시간을 두고 N번 재시도.
        List<CurrentPrice> prices = currentPriceRepository.findByStockIds(stockIds);

        return prices.stream()
            .map(CurrentPriceDto.Response::from)
            .toList();
    }

    @Override
    public List<StockDto.Response> getTopStocksByValue(Pageable pageable) {
        List<StockDto.TopStockResponse> topStockResponseList =  realMarketClient.getTopStocksByValue(100);

        return mapToResponseWithStocks(topStockResponseList);
    }

    @Override
    public List<StockDto.Response> getTopStocksByVolume(Pageable pageable) {
        List<StockDto.TopStockResponse> topStockResponseList = realMarketClient.getTopStocksByVolume(100);
        return mapToResponseWithStocks(topStockResponseList);
    }

    @Override
    public List<StockDto.Response> getTopRisingStocks(Pageable pageable) {
        List<StockDto.TopStockResponse> topStockResponseList =  realMarketClient.getTopRisingStocks(100);
        return mapToResponseWithStocks(topStockResponseList);
    }

    @Override
    public List<StockDto.Response> getTopFallingStocks(Pageable pageable) {
        List<StockDto.TopStockResponse> topStockResponseList =  realMarketClient.getTopFallingStocks(100);
        return mapToResponseWithStocks(topStockResponseList);
    }

    private void checkStockIsExist(Long stockId) {
        if(!stockRepository.existsById(stockId)) {
            throw new DomainException(MarketErrorCode.STOCK_NOT_FOUND);
        }
    }

    private @NonNull List<StockDto.Response> mapToResponseWithStocks(List<StockDto.TopStockResponse> topStockResponseList) {
        List<String> symbols = topStockResponseList.stream()
            .map(StockDto.TopStockResponse::getSymbol)
            .toList();

        List<Stock> stocks = stockRepository.findAllBySymbolIn(symbols);

        Map<String, Stock> stockMap = stocks.stream()
            .collect(Collectors.toMap(Stock::getSymbol, stock -> stock));

        return topStockResponseList.stream()
            .map(topStock -> {
                Stock stock = stockMap.get(topStock.getSymbol());
                if (stock == null) {
                    throw new DomainException(MarketErrorCode.STOCK_NOT_FOUND);
                }
                return StockDto.Response.from(stock);
            })
            .toList();
    }
}
