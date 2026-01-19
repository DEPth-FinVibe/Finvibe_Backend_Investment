package depth.finvibe.investment.modules.market.infra.client;

import depth.finvibe.investment.modules.market.application.port.out.RealMarketClient;
import depth.finvibe.investment.modules.market.domain.enums.RankType;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import depth.finvibe.investment.modules.market.dto.PriceCandleDto;
import depth.finvibe.investment.modules.market.dto.StockDto.RankingResponse;
import depth.finvibe.investment.modules.market.dto.StockDto.RealMarketResponse;
import depth.finvibe.investment.modules.market.infra.client.dto.KisDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RealMarketClientImpl implements RealMarketClient {

    private final KisApiClient kisApiClient;

    @Override
    public List<PriceCandleDto.Response> fetchPriceCandles(Long stockId, List<LocalDateTime> missingCandleTimes, Timeframe timeframe) {
        return List.of();
    }

    @Override
    public List<RealMarketResponse> fetchStocksInKOSPI() {

        return List.of();
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
}

