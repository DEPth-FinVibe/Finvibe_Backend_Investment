package depth.finvibe.investment.modules.asset.api;

import depth.finvibe.investment.boot.config.WebMvcConfig;
import depth.finvibe.investment.boot.security.model.UserRole;
import depth.finvibe.investment.boot.security.resolver.JwtArgumentResolver;
import depth.finvibe.investment.modules.asset.api.external.AssetController;
import depth.finvibe.investment.modules.asset.application.port.in.AssetCommandUseCase;
import depth.finvibe.investment.modules.asset.application.port.in.AssetQueryUseCase;
import depth.finvibe.investment.modules.asset.domain.Currency;
import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssetController.class)
@Import({WebMvcConfig.class, JwtArgumentResolver.class})
public class AssetControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssetQueryUseCase assetQueryUseCase;

    @MockitoBean
    private AssetCommandUseCase commandUseCase;

    @Test
    @DisplayName("포트폴리오 자산 조회 API")
    void getAssetsByPortfolio() throws Exception {
        // given
        Long portfolioId = 1L;
        UUID userId = UUID.randomUUID();

        // 가짜 반환 데이터 생성 (List<AssetResponse>)
        PortfolioGroupDto.AssetResponse asset1 = PortfolioGroupDto.AssetResponse.builder()
                .id(101L)
                .name("Samsung Electronics")
                .amount(new BigDecimal("10"))
                .totalPrice(new BigDecimal("750000"))
                .currency(Currency.KRW)
                .stockId(10L)
                .build();

        PortfolioGroupDto.AssetResponse asset2 = PortfolioGroupDto.AssetResponse.builder()
                .id(102L)
                .name("Apple Inc.")
                .amount(new BigDecimal("5"))
                .totalPrice(new BigDecimal("900"))
                .currency(Currency.USD)
                .stockId(20L)
                .build();

        List<PortfolioGroupDto.AssetResponse> mockResponseList = List.of(asset1, asset2);

        given(assetQueryUseCase.getAssetsByPortfolio(portfolioId, userId))
                .willReturn(mockResponseList);

        // when
        mockMvc.perform(get("/portfolios/{portfolioId}/assets", portfolioId)
                        .header("Authorization", bearerToken(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Samsung Electronics"))
                .andExpect(jsonPath("$[0].currency").value("KRW"))
                .andExpect(jsonPath("$[1].name").value("Apple Inc."));
    }

    @Test
    @DisplayName("전체 자산 배분 조회 API")
    void getAssetAllocation() throws Exception {
        UUID userId = UUID.randomUUID();
        PortfolioGroupDto.AssetAllocationResponse response = PortfolioGroupDto.AssetAllocationResponse.builder()
                .cashAmount(new BigDecimal("2500000"))
                .stockAmount(new BigDecimal("8300000"))
                .totalAmount(new BigDecimal("10800000"))
                .changeAmount(new BigDecimal("800000"))
                .changeRate(new BigDecimal("8.0000"))
                .build();

        given(assetQueryUseCase.getAssetAllocation(userId)).willReturn(response);

        mockMvc.perform(get("/assets/allocation")
                        .header("Authorization", bearerToken(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashAmount").value(2500000))
                .andExpect(jsonPath("$.stockAmount").value(8300000))
                .andExpect(jsonPath("$.totalAmount").value(10800000))
                .andExpect(jsonPath("$.changeAmount").value(800000))
                .andExpect(jsonPath("$.changeRate").value(8.0000));
    }

    @Test
    @DisplayName("포트폴리오별 수익 비교 조회 API")
    void getPortfolioComparisons() throws Exception {
        UUID userId = UUID.randomUUID();
        List<PortfolioGroupDto.PortfolioComparisonResponse> response = List.of(
                PortfolioGroupDto.PortfolioComparisonResponse.builder()
                        .name("공격형")
                        .totalAssetAmount(new BigDecimal("1500000"))
                        .returnRate(new BigDecimal("12.5000"))
                        .realizedProfit(new BigDecimal("180000"))
                        .build(),
                PortfolioGroupDto.PortfolioComparisonResponse.builder()
                        .name("안정형")
                        .totalAssetAmount(new BigDecimal("900000"))
                        .returnRate(new BigDecimal("4.2000"))
                        .realizedProfit(new BigDecimal("36000"))
                        .build()
        );

        given(assetQueryUseCase.getPortfolioComparisons(userId)).willReturn(response);

        mockMvc.perform(get("/portfolios/comparison")
                        .header("Authorization", bearerToken(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("공격형"))
                .andExpect(jsonPath("$[0].totalAssetAmount").value(1500000))
                .andExpect(jsonPath("$[0].returnRate").value(12.5000))
                .andExpect(jsonPath("$[0].realizedProfit").value(180000));
    }

    private String bearerToken(UUID userId) throws Exception {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(Map.of(
                        "id", userId.toString(),
                        "role", UserRole.USER.name()
                )));
        return "Bearer " + header + "." + payload + ".sig";
    }
}
