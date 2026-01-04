package depth.finvibe.investment.modules.asset.api;

import depth.finvibe.investment.modules.asset.application.port.in.AssetCommandUseCase;
import depth.finvibe.investment.modules.asset.application.port.in.AssetQueryUseCase;
import depth.finvibe.investment.modules.asset.domain.Currency;
import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
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
                        .param("userId", userId.toString())
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
    @DisplayName("자산 등록 API")
    void registerAsset() throws Exception {
        // given
        Long portfolioId = 1L;
        UUID userId = UUID.randomUUID();

        PortfolioGroupDto.RegisterAssetRequest requestDto = PortfolioGroupDto.RegisterAssetRequest.builder()
                .stockId(105L)
                .name("Samsung Electronics")
                .amount(new BigDecimal("10.5"))
                .stockPrice(new BigDecimal("75000"))
                .currency(Currency.KRW)
                .build();

        // when
        mockMvc.perform(post("/portfolios/{portfolioId}/assets", portfolioId)
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated());

        // then
        verify(commandUseCase).registerAsset(
                eq(portfolioId),
                any(PortfolioGroupDto.RegisterAssetRequest.class),
                eq(userId)
        );
    }

    @Test
    @DisplayName("자산 삭제 API")
    void unregisterAsset() throws Exception {
        // given
        Long portfolioId = 1L;
        UUID userId = UUID.randomUUID();

        // 삭제 요청 DTO 생성
        PortfolioGroupDto.UnregisterAssetRequest requestDto = PortfolioGroupDto.UnregisterAssetRequest.builder()
                .stockId(105L)
                .amount(new BigDecimal("5.0"))
                .stockPrice(new BigDecimal("75000"))
                .currency(Currency.KRW)
                .build();

        // when
        mockMvc.perform(delete("/portfolios/{portfolioId}/assets", portfolioId)
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isNoContent());

        // then
        verify(commandUseCase).unregisterAsset(
                eq(portfolioId),
                any(PortfolioGroupDto.UnregisterAssetRequest.class),
                eq(userId)
        );
    }
}