package depth.finvibe.investment.modules.market.application;

import depth.finvibe.investment.modules.market.application.port.in.MarketQueryUseCase;
import depth.finvibe.investment.modules.market.application.port.out.CurrentPriceRepository;
import depth.finvibe.investment.modules.market.application.port.out.PriceCandleRepository;
import depth.finvibe.investment.modules.market.application.port.out.RealMarketClient;
import depth.finvibe.investment.modules.market.application.port.out.RealtimeStockIndexRepository;
import depth.finvibe.investment.modules.market.application.port.out.StockRankingRepository;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.CurrentPrice;
import depth.finvibe.investment.modules.market.domain.PriceCandle;
import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.domain.StockRanking;
import depth.finvibe.investment.modules.market.domain.enums.RankType;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import depth.finvibe.investment.modules.market.domain.error.MarketErrorCode;
import depth.finvibe.investment.modules.market.dto.CurrentPriceDto;
import depth.finvibe.investment.modules.market.dto.PriceCandleDto;
import depth.finvibe.investment.modules.market.dto.StockDto;
import depth.finvibe.investment.shared.error.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class MarketQueryService implements MarketQueryUseCase {

    private final PriceCandleRepository priceCandleRepository;
    private final RealMarketClient realMarketClient;
    private final CurrentPriceRepository currentPriceRepository;
    private final RealtimeStockIndexRepository realtimeStockIndexRepository;
    private final StockRankingRepository stockRankingRepository;
    private final StockRepository stockRepository;

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
            case Timeframe.WEEK:
                for (int i = 0; i < count; i++) {
                    candleTimes.add(startTime.plusWeeks(i)
                            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                            .withHour(0).withMinute(0).withSecond(0).withNano(0));
                }
                break;
            case Timeframe.MONTH:
                for (int i = 0; i < count; i++) {
                    candleTimes.add(startTime.plusMonths(i)
                            .with(TemporalAdjusters.firstDayOfMonth())
                            .withHour(0).withMinute(0).withSecond(0).withNano(0));
                }
                break;
            case Timeframe.YEAR:
                for (int i = 0; i < count; i++) {
                    candleTimes.add(startTime.plusYears(i)
                            .with(TemporalAdjusters.firstDayOfYear())
                            .withHour(0).withMinute(0).withSecond(0).withNano(0));
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
    public List<StockDto.Response> getTopStocksByValue() {
        return getTopStocksByRankType(RankType.TOP_VALUE);
    }

    @Override
    public List<StockDto.Response> getTopStocksByVolume() {
        return getTopStocksByRankType(RankType.TOP_VOLUME);
    }

    @Override
    public List<StockDto.Response> getTopRisingStocks() {
        return getTopStocksByRankType(RankType.TOP_RISING);
    }

    @Override
    public List<StockDto.Response> getTopFallingStocks() {
        return getTopStocksByRankType(RankType.TOP_FALLING);
    }

    private List<StockDto.Response> getTopStocksByRankType(RankType rankType) {
        List<StockRanking> rankings = stockRankingRepository.findByRankType(rankType);
        
        List<Long> stockIds = rankings.stream()
                .map(StockRanking::getStockId)
                .toList();
        
        List<Stock> stocks = stockRepository.findAllById(stockIds);
        
        Map<Long, Stock> stockMap = stocks.stream()
                .collect(Collectors.toMap(Stock::getId, stock -> stock));
        
        return rankings.stream()
                .map(ranking -> stockMap.get(ranking.getStockId()))
                .filter(Objects::nonNull)
                .map(StockDto.Response::from)
                .toList();
    }
}
