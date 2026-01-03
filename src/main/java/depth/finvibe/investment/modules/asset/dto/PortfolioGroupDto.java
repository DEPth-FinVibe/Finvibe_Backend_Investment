package depth.finvibe.investment.modules.asset.dto;

import java.math.BigDecimal;
import depth.finvibe.investment.modules.asset.domain.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class PortfolioGroupDto {

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class PortfolioGroupResponse {
        private Long id;
        private String name;
        private String iconCode;
    }
    
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class CreatePortfolioGroupRequest {
        private String name;
        private String iconCode;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class UpdatePortfolioGroupRequest {
        private String name;
        private String iconCode;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class RegisterAssetRequest {
        private Long stockId;
        private BigDecimal amount;
        private BigDecimal stockPrice;
        private String name;
        private Currency currency;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class UnregisterAssetRequest {
        private Long stockId;
        private BigDecimal amount;
        private BigDecimal stockPrice;
        private Currency currency;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class AssetResponse {
        private Long id;
        private String name;
        private BigDecimal amount;
        private BigDecimal totalPrice;
        private Currency currency;
        private Long stockId;
    }
}
