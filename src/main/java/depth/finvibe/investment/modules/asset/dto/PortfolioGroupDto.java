package depth.finvibe.investment.modules.asset.dto;

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
}
