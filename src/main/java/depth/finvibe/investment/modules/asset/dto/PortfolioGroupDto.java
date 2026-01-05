package depth.finvibe.investment.modules.asset.dto;

import java.math.BigDecimal;

import depth.finvibe.investment.modules.asset.domain.Asset;
import depth.finvibe.investment.modules.asset.domain.Currency;
import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;
import jakarta.validation.constraints.NotBlank;
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

        public static PortfolioGroupResponse from(PortfolioGroup portfolioGroup) {
            return PortfolioGroupResponse.builder()
                    .id(portfolioGroup.getId())
                    .name(portfolioGroup.getName())
                    .iconCode(portfolioGroup.getIconCode())
                    .build();
        }
    }
    
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class CreatePortfolioGroupRequest {
        @NotBlank
        private String name;
        @NotBlank
        private String iconCode;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class UpdatePortfolioGroupRequest {
        @NotBlank
        private String name;
        @NotBlank
        private String iconCode;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class RegisterAssetRequest {
        @NotBlank
        private Long stockId;
        @NotBlank
        private BigDecimal amount;
        @NotBlank
        private BigDecimal stockPrice;
        @NotBlank
        private String name;
        @NotBlank
        private Currency currency;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class UnregisterAssetRequest {
        @NotBlank
        private Long stockId;
        @NotBlank
        private BigDecimal amount;
        @NotBlank
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

        public static AssetResponse from(Asset asset) {
            return AssetResponse.builder()
                    .id(asset.getId())
                    .name(asset.getName())
                    .amount(asset.getAmount())
                    .totalPrice(asset.getTotalPrice().getAmount())
                    .currency(asset.getTotalPrice().getCurrency())
                    .stockId(asset.getStockId())
                    .build();
        }
    }
}
