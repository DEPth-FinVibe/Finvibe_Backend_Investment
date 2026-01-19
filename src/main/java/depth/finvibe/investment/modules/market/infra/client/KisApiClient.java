package depth.finvibe.investment.modules.market.infra.client;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import depth.finvibe.investment.modules.market.infra.client.dto.KisDto;

import java.util.List;
import java.util.Objects;

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
