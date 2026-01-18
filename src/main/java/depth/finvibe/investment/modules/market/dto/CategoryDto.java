package depth.finvibe.investment.modules.market.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class CategoryDto {

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class Response {
        private Long categoryId;
        private String categoryName;
    }
}
