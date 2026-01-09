package depth.finvibe.investment.modules.trade.api;

import depth.finvibe.investment.modules.trade.api.external.TradeController;
import depth.finvibe.investment.modules.trade.application.port.in.TradeCommandUseCase;
import depth.finvibe.investment.modules.trade.application.port.in.TradeQueryUseCase;
import depth.finvibe.investment.modules.trade.domain.enums.MarketType;
import depth.finvibe.investment.modules.trade.domain.enums.TradeType;
import depth.finvibe.investment.modules.trade.domain.enums.TransactionType;
import depth.finvibe.investment.modules.trade.dto.TradeDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TradeController.class)
class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TradeCommandUseCase tradeCommandUseCase;

    @MockitoBean
    private TradeQueryUseCase tradeQueryUseCase;

    @Test
    @DisplayName("주문 상태 조회 성공")
    void getTradeStatus() throws Exception {
        // given
        Long tradeId = 1L;
        UUID userId = UUID.randomUUID();

        TradeDto.TradeResponse mockResponse = TradeDto.TradeResponse.builder()
                .tradeId(tradeId)
                .stockId(100L)
                .amount(10.5)
                .price(50000L)
                .portfolioId(1L)
                .userId(userId)
                .marketType(MarketType.DOMESTIC)
                .tradeType(TradeType.NORMAL)
                .transactionType(TransactionType.BUY)
                .build();

        given(tradeQueryUseCase.findTrade(tradeId)).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/external/trades/{tradeId}", tradeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeId").value(tradeId))
                .andExpect(jsonPath("$.amount").value(10.5))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andDo(print());
    }

    @Test
    @DisplayName("매수/매도 주문 체결 요청 성공")
    void placeTrade() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        TradeDto.TransactionRequest request = TradeDto.TransactionRequest.builder()
                .marketType(MarketType.DOMESTIC)
                .stockId(100L)
                .amount(5.0)
                .price(50000L)
                .portfolioId(10L)
                .userId(userId)
                .tradeType(TradeType.NORMAL)
                .transactionType(TransactionType.BUY)
                .build();

        TradeDto.TradeResponse mockResponse = TradeDto.TradeResponse.builder()
                .tradeId(1L)
                .stockId(100L)
                .amount(5.0)
                .price(50000L)
                .userId(userId)
                .transactionType(TransactionType.BUY)
                .build();

        given(tradeCommandUseCase.createTrade(any(TradeDto.TransactionRequest.class)))
                .willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/external/trades")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeId").value(1L))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andDo(print());
    }

    @Test
    @DisplayName("예약 주문 취소 성공")
    void cancelTrade() throws Exception {
        // given
        Long tradeId = 1L;

        TradeDto.TradeResponse mockResponse = TradeDto.TradeResponse.builder()
                .tradeId(tradeId)
                .tradeType(TradeType.CANCELLED)
                .build();

        given(tradeCommandUseCase.cancelTrade(tradeId)).willReturn(mockResponse);

        // when & then
        mockMvc.perform(delete("/external/trades/{tradeId}", tradeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());

        verify(tradeCommandUseCase).cancelTrade(eq(tradeId));
    }
}