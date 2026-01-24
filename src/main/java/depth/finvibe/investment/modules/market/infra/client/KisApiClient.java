package depth.finvibe.investment.modules.market.infra.client;

import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import depth.finvibe.investment.modules.market.infra.client.dto.KisDto;

/**
 * 한국투자증권 Open API 클라이언트
 */
@Component
public class KisApiClient {

    private final RestClient restClient;
    private final String kisUserId;

    public KisApiClient(
        @Qualifier("kisRestClient")
        RestClient restClient,
        @Value("${market.kis.user-id}")
        String kisUserId
    ) {
        this.restClient = restClient;
        this.kisUserId = kisUserId;
    }


    /**
     * <a href="https://apiportal.koreainvestment.com/apiservice-apiservice?/uapi/domestic-stock/v1/quotations/psearch-result">종목조건검색조회 API</a>
     * @param condition 조건 번호
     * @return 조건에 해당하는 종목 리스트
     */
    public List<KisDto.ConditionalStockSearchResponseItem> fetchConditionalStockSearch(ConditionSeq condition) {
        return Objects.requireNonNull(
                    restClient.get()
                            .uri("/uapi/domestic-stock/v1/quotations/psearch-result" +
                                    "?user_id=" + kisUserId +
                                    "&seq=" + condition.getSeq())
                            .headers(h -> {
                                h.set("tr_id", "HHKST03900400");
                            })
                            .retrieve()
                            .body(KisDto.ConditionalStockSearchResponse.class)
                )
                .getOutput2();
    }

    /**
     * <a href="https://apiportal.koreainvestment.com/apiservice-apiservice?/uapi/domestic-stock/v1/quotations/inquire-time-dailychartprice">주식일별분봉조회 API</a>
     * 최대 120개의 분봉만 한번에 조회할 수 있음.
     * 조회할 시간부터 2시간 전의 시간까지 조회됨 (예: 130000 조회 시 130000~110000 1분 단위로 120개 조회됨, 순서는 최신 데이터가 먼저)
     */
    public KisDto.TimeDailyChartPriceResponse fetchTimeDailyChartPrice(
            String marketCode,
            String stockCode,
            String time, //조회할 시간 : HHMMSS
            String date, //조회할 일자 : YYYYMMDD
            String includePastData,
            String includeFakeTick
    ) {
        String pastDataIncu = includePastData == null ? "N" : includePastData;
        String fakeTickIncu = includeFakeTick == null ? "" : includeFakeTick;

        return Objects.requireNonNull(
                restClient.get()
                        .uri("/uapi/domestic-stock/v1/quotations/inquire-time-dailychartprice" +
                                "?FID_COND_MRKT_DIV_CODE=" + marketCode +
                                "&FID_INPUT_ISCD=" + stockCode +
                                "&FID_INPUT_HOUR_1=" + time +
                                "&FID_INPUT_DATE_1=" + date +
                                "&FID_PW_DATA_INCU_YN=" + pastDataIncu +
                                "&FID_FAKE_TICK_INCU_YN=" + fakeTickIncu)
                        .headers(h -> h.set("tr_id", "FHKST03010230"))
                        .retrieve()
                        .body(KisDto.TimeDailyChartPriceResponse.class)
        );
    }

    /**
     * <a href="https://apiportal.koreainvestment.com/apiservice-apiservice?/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice">국내주식기간별시세(일/주/월/년) API</a>
     */
    public KisDto.DailyItemChartPriceResponse fetchDailyItemChartPrice(
            String marketCode,
            String stockCode,
            String startDate,
            String endDate,
            String periodCode,
            String originalAdjustedPriceFlag
    ) {
        return Objects.requireNonNull(
                restClient.get()
                        .uri("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice" +
                                "?FID_COND_MRKT_DIV_CODE=" + marketCode +
                                "&FID_INPUT_ISCD=" + stockCode +
                                "&FID_INPUT_DATE_1=" + startDate +
                                "&FID_INPUT_DATE_2=" + endDate +
                                "&FID_PERIOD_DIV_CODE=" + periodCode +
                                "&FID_ORG_ADJ_PRC=" + originalAdjustedPriceFlag)
                        .headers(h -> h.set("tr_id", "FHKST03010100"))
                        .retrieve()
                        .body(KisDto.DailyItemChartPriceResponse.class)
        );
    }

    /**
     * <a href="https://apiportal.koreainvestment.com/apiservice-apiservice?/uapi/domestic-stock/v1/quotations/intstock-multprice">관심종목(멀티종목) 시세조회 API [국내주식-205]</a>
     * 한 번의 호출에 최대 30종목의 시세 확인 가능합니다.
     * @param stocks 종목 정보 리스트 (최대 30개)
     * @return 관심종목 시세 리스트
     */
    public List<KisDto.IntstockMultpriceResponseItem> fetchIntstockMultprice(List<KisDto.StockInfo> stocks) {
        if (stocks == null || stocks.isEmpty()) {
            return List.of();
        }
        if (stocks.size() > 30) {
            throw new IllegalArgumentException("최대 30종목까지 조회 가능합니다.");
        }

        StringBuilder uriBuilder = new StringBuilder("/uapi/domestic-stock/v1/quotations/intstock-multprice?");
        for (int i = 0; i < stocks.size(); i++) {
            if (i > 0) {
                uriBuilder.append("&");
            }
            KisDto.StockInfo stock = stocks.get(i);
            uriBuilder.append("FID_COND_MRKT_DIV_CODE_").append(i + 1).append("=").append(stock.getMarketCode());
            uriBuilder.append("&");
            uriBuilder.append("FID_INPUT_ISCD_").append(i + 1).append("=").append(stock.getStockCode());
        }

        return Objects.requireNonNull(
                restClient.get()
                        .uri(uriBuilder.toString())
                        .headers(h -> h.set("tr_id", "FHKST11300006"))
                        .retrieve()
                        .body(KisDto.IntstockMultpriceResponse.class)
        ).getOutput();
    }

    @RequiredArgsConstructor
    @Getter
    public enum ConditionSeq {
        TRADE_VALUE(0), // 거래대금
        VOLUME(1),    // 거래량
        RISE_RATE(2),  // 상승율
        FALL_RATE(3);  // 하락율

        private final int seq;
    }


}
