package depth.finvibe.investment.modules.market.infra.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class KisDto {

    public static class ConditionalStockSearchRequest {

    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class ConditionalStockSearchResponse {
        private String rt_cd;
        private String msg_cd;
        private String msg1;
        private List<ConditionalStockSearchResponseItem> output2;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class ConditionalStockSearchResponseItem {
        private String code;         // 종목코드
        private String name;         // 종목명
        private String daebi;        // 전일대비부호
        private String price;        // 현재가
        private String chgrate;      // 등락율
        private String acml_vol;     // 거래량
        private String trade_amt;    // 거래대금
        private String change;       // 전일대비
        private String cttr;         // 체결강도
        private String open;         // 시가
        private String high;         // 고가
        private String low;          // 저가
        private String high52;       // 52주최고가
        private String low52;        // 52주최저가
        private String expprice;     // 예상체결가
        private String expchange;    // 예상대비
        private String expchggrate;  // 예상등락률
        private String expcvol;      // 예상체결수량
        private String chgrate2;     // 전일거래량대비율
        private String expdaebi;     // 예상대비부호
        private String recprice;     // 기준가
        private String uplmtprice;   // 상한가
        private String dnlmtprice;   // 하한가
        private String stotprice;    // 시가총액
    }
}
