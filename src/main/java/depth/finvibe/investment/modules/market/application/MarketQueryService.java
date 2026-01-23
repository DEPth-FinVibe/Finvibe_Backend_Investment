package depth.finvibe.investment.modules.market.application;

import depth.finvibe.investment.modules.market.application.port.in.MarketQueryUseCase;
import depth.finvibe.investment.modules.market.application.port.out.CurrentPriceRepository;
import depth.finvibe.investment.modules.market.application.port.out.PriceCandleRepository;
import depth.finvibe.investment.modules.market.application.port.out.RealMarketClient;
import depth.finvibe.investment.modules.market.application.port.out.CurrentStockWatcherRepository;
import depth.finvibe.investment.modules.market.application.port.out.StockRankingRepository;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.CurrentPrice;
import depth.finvibe.investment.modules.market.domain.PriceCandle;
import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.domain.StockRanking;
import depth.finvibe.investment.modules.market.domain.enums.MarketSearchType;
import depth.finvibe.investment.modules.market.domain.enums.MarketType;
import depth.finvibe.investment.modules.market.domain.enums.RankType;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import depth.finvibe.investment.modules.market.domain.error.MarketErrorCode;
import depth.finvibe.investment.modules.market.dto.CurrentPriceDto;
import depth.finvibe.investment.modules.market.dto.PriceCandleDto;
import depth.finvibe.investment.modules.market.dto.StockDto;
import depth.finvibe.investment.shared.error.DomainException;
import depth.finvibe.investment.shared.lock.DistributedLockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketQueryService implements MarketQueryUseCase {

    private final PriceCandleRepository priceCandleRepository;
    private final RealMarketClient realMarketClient;
    private final CurrentPriceRepository currentPriceRepository;
    private final CurrentStockWatcherRepository currentStockWatcherRepository;
    private final StockRankingRepository stockRankingRepository;
    private final StockRepository stockRepository;
    private final DistributedLockManager distributedLockManager;

    @Override
    @Transactional
    public List<PriceCandleDto.Response> getStockCandles(Long stockId, LocalDateTime startTime, LocalDateTime endTime, Timeframe timeframe) {
        
        // 분산 락 키: 종목ID + 시간프레임 (단일 락)
        String lockKey = String.format("stock:candle:%d:%s", stockId, timeframe);
        
        // 분산 락 적용: 대기 10초, 보유 60초 (API 호출 및 배치 저장 고려)
        return distributedLockManager.executeWithLock(
                lockKey,
                Duration.ofSeconds(10),
                Duration.ofSeconds(60),
                () -> fetchStockCandlesWithLock(stockId, startTime, endTime, timeframe)
        );
    }

    /**
     * 분산 락 내에서 실행되는 실제 캔들 조회 로직
     * 락 내에서 DB를 다시 조회하여 다른 노드가 이미 저장했는지 확인
     */
    private List<PriceCandleDto.Response> fetchStockCandlesWithLock(
            Long stockId, LocalDateTime startTime, LocalDateTime endTime, Timeframe timeframe) {

        // 1. Timeframe에 따라 시간 범위 정규화
        LocalDateTime normalizedStart = timeframe.normalizeStart(startTime);
        LocalDateTime normalizedEnd = timeframe.normalizeEnd(endTime);

        // 2. DB에서 기존 캔들 조회 (락 내에서 다시 조회!)
        List<PriceCandle> existingCandles = priceCandleRepository.findExisting(stockId, normalizedStart, normalizedEnd, timeframe);

        // 3. 없는 캔들만 계산
        List<LocalDateTime> missingCandleTimes = calculateMissingCandleTimes(normalizedStart, normalizedEnd, timeframe, existingCandles);
        
        List<PriceCandleDto.Response> fetchedCandles = new ArrayList<>();
        if (!missingCandleTimes.isEmpty()) {

            // 4. 가져와야 하는 캔들의 시간 범위 계산
            LocalDateTime earliestTime = missingCandleTimes.stream().min(LocalDateTime::compareTo).orElse(normalizedStart);
            LocalDateTime latestTime = missingCandleTimes.stream().max(LocalDateTime::compareTo).orElse(normalizedEnd);
            
            // 5. 해당 범위의 모든 캔들 시각 생성
            List<LocalDateTime> allCandleTimesInRange = generateCandleTimesInRange(earliestTime, latestTime, timeframe);
            
            // 6. RealMarketClient가 내부적으로 청킹 처리
            fetchedCandles = realMarketClient.fetchPriceCandles(stockId, earliestTime, latestTime, timeframe);
            
            // 7. API에서 받은 캔들 배치 저장 + 못 받은 캔들은 isMissing=true로 저장
            Set<LocalDateTime> existingCandleTimes = existingCandles.stream()
                    .map(PriceCandle::getAt)
                    .collect(Collectors.toSet());
            saveFetchedAndMissingCandles(fetchedCandles, allCandleTimesInRange, stockId, timeframe, existingCandleTimes);
        }

        // 8. 결과 병합 및 반환
        return mergeAndSortCandles(existingCandles, fetchedCandles);
    }

    private List<PriceCandleDto.Response> mergeAndSortCandles(List<PriceCandle> existingCandles, List<PriceCandleDto.Response> fetchedCandles) {
        // isMissing=true인 캔들은 제외하고 실제 데이터만 변환
        List<PriceCandleDto.Response> existingCandleDtos = existingCandles.stream()
                .filter(candle -> !candle.getIsMissing())
                .map(PriceCandleDto.Response::from)
                .toList();

        // DB에서 가져온 캔들의 시각을 Set으로 변환 (중복 체크용)
        Set<LocalDateTime> existingTimes = existingCandleDtos.stream()
                .map(PriceCandleDto.Response::getAt)
                .collect(Collectors.toSet());

        // API에서 가져온 캔들 중 DB에 없는 것만 필터링
        List<PriceCandleDto.Response> uniqueFetchedCandles = fetchedCandles.stream()
                .filter(candle -> !existingTimes.contains(candle.getAt()))
                .toList();

        // 중복 제거된 결과를 병합하고 정렬
        return Stream.concat(existingCandleDtos.stream(), uniqueFetchedCandles.stream())
                .sorted(Comparator.comparing(PriceCandleDto.Response::getAt))
                .toList();
    }

    private void saveFetchedAndMissingCandles(
            List<PriceCandleDto.Response> fetchedCandles, 
            List<LocalDateTime> allCandleTimesInRange,
            Long stockId, 
            Timeframe timeframe,
            Set<LocalDateTime> existingTimes) {
        
        // fetch된 캔들들의 시각 추출
        List<LocalDateTime> fetchedTimes = fetchedCandles.stream()
                .map(PriceCandleDto.Response::getAt)
                .toList();

        // 1. API에서 받은 캔들 중 DB에 없는 것만 필터링
        List<PriceCandle> newCandles = fetchedCandles.stream()
                .filter(dto -> !existingTimes.contains(dto.getAt()))
                .map(this::createPriceCandleFrom)
                .collect(Collectors.toCollection(ArrayList::new));

        // 2. API에서 못 받은 시각들 찾기
        Set<LocalDateTime> fetchedTimeSet = new HashSet<>(fetchedTimes);
        List<PriceCandle> missingCandles = allCandleTimesInRange.stream()
                .filter(time -> !fetchedTimeSet.contains(time) && !existingTimes.contains(time))
                .map(time -> PriceCandle.createMissing(stockId, timeframe, time))
                .toList();

        // 3. 실제 캔들 + 빈 캔들 모두 배치 저장
        newCandles.addAll(missingCandles);

        if (!newCandles.isEmpty()) {
            priceCandleRepository.saveAll(newCandles);
        }
    }

    private PriceCandle createPriceCandleFrom(PriceCandleDto.Response dto) {
        return PriceCandle.builder()
                .stockId(dto.getStockId())
                .timeframe(dto.getTimeframe())
                .at(dto.getAt())
                .isMissing(false)
                .open(dto.getOpen())
                .close(dto.getClose())
                .high(dto.getHigh())
                .low(dto.getLow())
                .volume(dto.getVolume())
                .value(dto.getValue())
                .prevDayChangePct(dto.getPrevDayChangePct())
                .build();
    }

    private List<LocalDateTime> calculateMissingCandleTimes(LocalDateTime startTime, LocalDateTime endTime, Timeframe timeframe, List<PriceCandle> existingCandles) {
        List<LocalDateTime> shouldHaveCandleTimes = generateCandleTimesInRange(startTime, endTime, timeframe);

        Set<LocalDateTime> existingCandleTimes = existingCandles.stream()
                .map(PriceCandle::getAt)
                .collect(Collectors.toSet());

        // DB에 존재하는 캔들(실제 데이터 + isMissing=true 모두) 제외
        return shouldHaveCandleTimes.stream()
                .filter(time -> !existingCandleTimes.contains(time))
                .toList();
    }

    private List<LocalDateTime> generateCandleTimesInRange(LocalDateTime startTime, LocalDateTime endTime, Timeframe timeframe) {
        List<LocalDateTime> candleTimes = new ArrayList<>();
        
        // startTime을 Timeframe에 맞춰 정규화 (방어적 처리)
        LocalDateTime current = timeframe.normalizeStart(startTime);

        while (!current.isAfter(endTime)) {
            candleTimes.add(current);
            current = timeframe.nextTime(current);
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
        if(!currentStockWatcherRepository.allExistsByStockIds(stockIds)) {
            throw new DomainException(MarketErrorCode.STOCK_NOT_FOUND);
        }

        // db에서 종목 정보도 함께 조회
        List<Stock> stocks = stockRepository.findAllById(stockIds);

        //인덱스에는 들어왔지만 실제로 값이 들어오지 않은 경우 예외가 발생. Infra에서 시간을 두고 N번 재시도.
        List<CurrentPrice> prices = currentPriceRepository.findByStockIds(stockIds);

        // 종목Id -> 종목 매핑
        Map<Long, Stock> stockMap = stocks.stream()
                .collect(Collectors.toMap(Stock::getId, stock -> stock));

        if (stockMap.size() != new HashSet<>(stockIds).size()) {
            Set<Long> missingStockIds = new HashSet<>(stockIds);
            missingStockIds.removeAll(stockMap.keySet());
            log.warn("Some stockIds are missing in DB. stockIds={}", missingStockIds);
        }

        // 반환
        return prices.stream()
                .filter(price -> stockMap.containsKey(price.getStockId()))
                .map(price -> CurrentPriceDto.Response.of(price, stockMap.get(price.getStockId())))
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

    @Override
    public List<StockDto.Response> searchStocks(String query, MarketSearchType marketType) {
        String trimmedQuery = query == null ? "" : query.trim();

        if (trimmedQuery.isEmpty()) {
            return List.of();
        }

        List<Stock> stocks = stockRepository.searchByNameOrSymbol(trimmedQuery);

        Stream<Stock> filteredStocks = switch (marketType) {
            case DOMESTIC -> stocks.stream().filter(stock -> stock.getMarketType() == MarketType.DOMESTIC);
            case INTERNATIONAL -> stocks.stream().filter(stock -> stock.getMarketType() == MarketType.INTERNATIONAL);
            case ALL -> stocks.stream();
        };

        return filteredStocks
                .map(StockDto.Response::from)
                .toList();
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
