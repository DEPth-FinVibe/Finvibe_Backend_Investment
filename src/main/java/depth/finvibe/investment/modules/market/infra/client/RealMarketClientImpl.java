package depth.finvibe.investment.modules.market.infra.client;

import depth.finvibe.investment.modules.market.application.port.out.RealMarketClient;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.domain.enums.RankType;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import depth.finvibe.investment.modules.market.domain.error.MarketErrorCode;
import depth.finvibe.investment.modules.market.dto.PriceCandleDto;
import depth.finvibe.investment.modules.market.dto.StockDto.RankingResponse;
import depth.finvibe.investment.modules.market.dto.StockDto.RealMarketResponse;
import depth.finvibe.investment.modules.market.infra.client.dto.KisDto;
import depth.finvibe.investment.shared.error.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RealMarketClientImpl implements RealMarketClient {

    private final KisApiClient kisApiClient;
    private final List<KisFileClient> kisFileClient;
    private final StockRepository stockRepository;

    @Override
    public List<PriceCandleDto.Response> fetchPriceCandles(Long stockId, LocalDateTime startTime, LocalDateTime endTime, Timeframe timeframe) {
        if (startTime == null || endTime == null) {
            return List.of();
        }

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new DomainException(MarketErrorCode.STOCK_NOT_FOUND));

        return switch (timeframe) {
            case MINUTE, HOUR -> fetchIntradayCandles(stock.getSymbol(), stockId, timeframe, startTime, endTime);
            case DAY, WEEK, MONTH, YEAR -> fetchDailyCandles(stock.getSymbol(), stockId, startTime, endTime, timeframe);
        };
    }

    @Override
    public List<RealMarketResponse> fetchStocksInRealMarket() {
        List<CompletableFuture<List<RealMarketResponse>>> futures = kisFileClient.stream()
                .map(client -> CompletableFuture.supplyAsync(client::fetchStocksInKisFile))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Override
    public List<RankingResponse> fetchStockRankings() {
        List<KisDto.ConditionalStockSearchResponseItem> valueTop100 = kisApiClient.fetchConditionalStockSearch(KisApiClient.ConditionSeq.TRADE_VALUE); // 거래대금 상위 100종목
        List<KisDto.ConditionalStockSearchResponseItem> volumeTop100 = kisApiClient.fetchConditionalStockSearch(KisApiClient.ConditionSeq.VOLUME); // 거래량 상위 100종목
        List<KisDto.ConditionalStockSearchResponseItem> risingTop100 = kisApiClient.fetchConditionalStockSearch(KisApiClient.ConditionSeq.RISE_RATE); // 상승률 상위 100종목
        List<KisDto.ConditionalStockSearchResponseItem> fallingTop100 = kisApiClient.fetchConditionalStockSearch(KisApiClient.ConditionSeq.FALL_RATE); // 하락률 상위 100종목

        List<RankingResponse> result = new ArrayList<>();

        addRankingResponses(result, valueTop100, RankType.TOP_VALUE);
        addRankingResponses(result, volumeTop100, RankType.TOP_VOLUME);
        addRankingResponses(result, risingTop100, RankType.TOP_RISING);
        addRankingResponses(result, fallingTop100, RankType.TOP_FALLING);

        return result;
    }

    private void addRankingResponses(List<RankingResponse> result,
                                   List<KisDto.ConditionalStockSearchResponseItem> items,
                                   RankType rankType) {
        for (int i = 0; i < items.size(); i++) {
            result.add(RankingResponse.builder()
                .symbol(items.get(i).getCode())
                .rankType(rankType)
                .rank(i + 1)
                .build());
        }
    }

    private List<PriceCandleDto.Response> fetchIntradayCandles(
            String symbol,
            Long stockId,
            Timeframe timeframe,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        // 시작 시각과 종료 시각 사이의 모든 날짜 수집
        Set<LocalDate> targetDates = new HashSet<>();
        LocalDate currentDate = startTime.toLocalDate();
        LocalDate lastDate = endTime.toLocalDate();
        
        while (!currentDate.isAfter(lastDate)) {
            targetDates.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }
        
        Map<LocalDateTime, PriceCandleDto.Response> results = new HashMap<>();

        for (LocalDate targetDate : targetDates) {
            String cursorDate = targetDate.format(DateTimeFormatter.BASIC_ISO_DATE);
            String cursorTime = "235959";

            int maxIterations = 20;
            for (int i = 0; i < maxIterations; i++) {
                KisDto.TimeDailyChartPriceResponse response = kisApiClient.fetchTimeDailyChartPrice(
                        "J",
                        symbol,
                        cursorTime,
                        cursorDate,
                        "Y",
                        ""
                );

                if (response == null || response.getOutput2() == null || response.getOutput2().isEmpty()) {
                    break;
                }

                LocalDateTime earliest = null;
                BigDecimal prevDayChangePct = response.getOutput1() == null
                        ? BigDecimal.ZERO
                        : toBigDecimal(response.getOutput1().getPrdy_ctrt());

                for (KisDto.TimeDailyChartPriceOutput2 item : response.getOutput2()) {
                    LocalDateTime candleAt = parseDateTime(item.getStck_bsop_date(), item.getStck_cntg_hour());
                    LocalDateTime normalizedAt = normalizeIntradayAt(candleAt, timeframe);

                    // 시간 범위 체크
                    if (normalizedAt.isBefore(startTime) || normalizedAt.isAfter(endTime)) {
                        earliest = earlier(earliest, candleAt);
                        continue;
                    }

                    if (results.containsKey(normalizedAt)) {
                        earliest = earlier(earliest, candleAt);
                        continue;
                    }

                    results.put(normalizedAt, PriceCandleDto.Response.builder()
                            .open(toBigDecimal(item.getStck_oprc()))
                            .close(toBigDecimal(item.getStck_prpr()))
                            .high(toBigDecimal(item.getStck_hgpr()))
                            .low(toBigDecimal(item.getStck_lwpr()))
                            .volume(toBigDecimal(item.getCntg_vol()))
                            .value(toBigDecimal(item.getAcml_tr_pbmn()))
                            .stockId(stockId)
                            .timeframe(timeframe)
                            .at(normalizedAt)
                            .prevDayChangePct(prevDayChangePct)
                            .build());

                    earliest = earlier(earliest, candleAt);
                }

                if (earliest == null) {
                    break;
                }

                LocalDateTime nextCursor = earliest.minusSeconds(1);
                if (nextCursor.toLocalDate().isBefore(targetDate)) {
                    break;
                }

                cursorDate = nextCursor.toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE);
                cursorTime = nextCursor.toLocalTime().format(DateTimeFormatter.ofPattern("HHmmss"));
            }
        }

        return new ArrayList<>(results.values());
    }

    private List<PriceCandleDto.Response> fetchDailyCandles(
            String symbol,
            Long stockId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Timeframe timeframe
    ) {
        Map<LocalDateTime, PriceCandleDto.Response> results = new HashMap<>();

        String periodCode = switch (timeframe) {
            case DAY -> "D";
            case WEEK -> "W";
            case MONTH -> "M";
            case YEAR -> "Y";
            default -> throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
        };

        // API는 최대 100개씩만 가져올 수 있으므로, 시작~종료 범위를 한번에 요청
        // (API가 내부적으로 100개 제한을 처리하는 것으로 가정)
        KisDto.DailyItemChartPriceResponse response = kisApiClient.fetchDailyItemChartPrice(
                "J",
                symbol,
                startTime.toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE),
                endTime.toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE),
                periodCode,
                "0"
        );

        if (response == null || response.getOutput2() == null) {
            return List.of();
        }

        for (KisDto.DailyItemChartPriceOutput2 item : response.getOutput2()) {
            LocalDateTime candleAt = parseDateTime(item.getStck_bsop_date(), null);
            LocalDateTime normalizedAt = normalizeDateAt(candleAt, timeframe);
            
            // 시간 범위 체크
            if (normalizedAt.isBefore(startTime) || normalizedAt.isAfter(endTime)) {
                continue;
            }

            results.put(normalizedAt, PriceCandleDto.Response.builder()
                    .open(toBigDecimal(item.getStck_oprc()))
                    .close(toBigDecimal(item.getStck_clpr()))
                    .high(toBigDecimal(item.getStck_hgpr()))
                    .low(toBigDecimal(item.getStck_lwpr()))
                    .volume(toBigDecimal(item.getAcml_vol()))
                    .value(toBigDecimal(item.getAcml_tr_pbmn()))
                    .stockId(stockId)
                    .timeframe(timeframe)
                    .at(normalizedAt)
                    .prevDayChangePct(BigDecimal.ZERO)
                    .build());
        }

        return new ArrayList<>(results.values());
    }

    private LocalDateTime normalizeIntradayAt(LocalDateTime at, Timeframe timeframe) {
        if (timeframe == Timeframe.HOUR) {
            return at.withMinute(0).withSecond(0).withNano(0);
        }
        return at.withSecond(0).withNano(0);
    }

    private LocalDateTime normalizeDateAt(LocalDateTime at, Timeframe timeframe) {
        return switch (timeframe) {
            case DAY -> at.withHour(0).withMinute(0).withSecond(0).withNano(0);
            case WEEK -> at.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
            case MONTH -> at.with(TemporalAdjusters.firstDayOfMonth())
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
            case YEAR -> at.with(TemporalAdjusters.firstDayOfYear())
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
            default -> at;
        };
    }

    private LocalDateTime parseDateTime(String date, String time) {
        LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE);
        if (time == null || time.isBlank()) {
            return parsedDate.atStartOfDay();
        }
        String normalizedTime = time.length() == 4 ? time + "00" : time;
        LocalTime parsedTime = LocalTime.parse(normalizedTime, DateTimeFormatter.ofPattern("HHmmss"));
        return LocalDateTime.of(parsedDate, parsedTime);
    }

    private BigDecimal toBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value);
    }

    private LocalDateTime earlier(LocalDateTime first, LocalDateTime second) {
        if (first == null) {
            return second;
        }
        return second.isBefore(first) ? second : first;
    }
}
