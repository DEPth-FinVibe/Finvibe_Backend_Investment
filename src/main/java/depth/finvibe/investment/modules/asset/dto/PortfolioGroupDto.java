package depth.finvibe.investment.modules.asset.dto;

import java.math.BigDecimal;

import depth.finvibe.investment.modules.asset.domain.Asset;
import depth.finvibe.investment.modules.asset.domain.Currency;
import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;
import jakarta.validation.constraints.NotNull;
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
        @NotNull
        private String name;
        @NotNull
        private String iconCode;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class UpdatePortfolioGroupRequest {
        @NotNull
        private String name;
        @NotNull
        private String iconCode;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class RegisterAssetRequest {
        @NotNull
        private Long stockId;
        @NotNull
        private BigDecimal amount;
        @NotNull
        private BigDecimal stockPrice;
        @NotNull
        private String name;
        @NotNull
        private Currency currency;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class UnregisterAssetRequest {
        @NotNull
        private Long stockId;
        @NotNull
        private BigDecimal amount;
        @NotNull
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
