package depth.finvibe.investment.modules.market.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class CategoryDto {

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    @Schema(name = "CategoryResponse", description = "카테고리 응답")
    public static class Response {
        @Schema(description = "카테고리 ID", example = "10")
        private Long categoryId;
        @Schema(description = "카테고리 이름", example = "반도체")
        private String categoryName;
    }
}
