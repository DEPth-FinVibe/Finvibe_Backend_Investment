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

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class TimeDailyChartPriceResponse {
        private String rt_cd;
        private String msg_cd;
        private String msg1;
        private TimeDailyChartPriceOutput1 output1;
        private List<TimeDailyChartPriceOutput2> output2;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class TimeDailyChartPriceOutput1 {
        private String prdy_vrss;
        private String prdy_vrss_sign;
        private String prdy_ctrt;
        private String stck_prdy_clpr;
        private String acml_vol;
        private String acml_tr_pbmn;
        private String hts_kor_isnm;
        private String stck_prpr;
        private String stck_bsop_date;
        private String stck_cntg_hour;
        private String stck_oprc;
        private String stck_hgpr;
        private String stck_lwpr;
        private String cntg_vol;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class TimeDailyChartPriceOutput2 {
        private String stck_bsop_date;
        private String stck_cntg_hour;
        private String stck_prpr;
        private String stck_oprc;
        private String stck_hgpr;
        private String stck_lwpr;
        private String cntg_vol;
        private String acml_vol;
        private String acml_tr_pbmn;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class DailyItemChartPriceResponse {
        private String rt_cd;
        private String msg_cd;
        private String msg1;
        private DailyItemChartPriceOutput1 output1;
        private List<DailyItemChartPriceOutput2> output2;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class DailyItemChartPriceOutput1 {
        private String prdy_vrss;
        private String prdy_vrss_sign;
        private String prdy_ctrt;
        private String stck_prdy_clpr;
        private String acml_vol;
        private String acml_tr_pbmn;
        private String hts_kor_isnm;
        private String stck_prpr;
        private String stck_shrn_iscd;
        private String prdy_vol;
        private String stck_mxpr;
        private String stck_llam;
        private String stck_oprc;
        private String stck_hgpr;
        private String stck_lwpr;
        private String stck_prdy_oprc;
        private String stck_prdy_hgpr;
        private String stck_prdy_lwpr;
        private String askp;
        private String bidp;
        private String prdy_vrss_vol;
        private String vol_tnrt;
        private String stck_fcam;
        private String lstn_stcn;
        private String cpfn;
        private String hts_avls;
        private String per;
        private String eps;
        private String pbr;
        private String itewhol_loan_rmnd_ratem;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class DailyItemChartPriceOutput2 {
        private String stck_bsop_date;
        private String stck_clpr;
        private String stck_oprc;
        private String stck_hgpr;
        private String stck_lwpr;
        private String acml_vol;
        private String acml_tr_pbmn;
        private String flng_cls_code;
        private String prtt_rate;
        private String mod_yn;
        private String prdy_vrss_sign;
        private String prdy_vrss;
        private String revl_issu_reas;
    }
}
