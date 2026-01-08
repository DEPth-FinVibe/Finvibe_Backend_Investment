package depth.finvibe.investment.modules.asset.api;

import depth.finvibe.investment.boot.config.WebMvcConfig;
import depth.finvibe.investment.boot.security.model.UserRole;
import depth.finvibe.investment.boot.security.resolver.JwtArgumentResolver;
import depth.finvibe.investment.modules.asset.application.port.in.AssetCommandUseCase;
import depth.finvibe.investment.modules.asset.application.port.in.AssetQueryUseCase;
import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

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

@WebMvcTest(PortfolioController.class)
@Import({WebMvcConfig.class, JwtArgumentResolver.class})
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssetCommandUseCase commandUseCase;

    @MockitoBean
    private AssetQueryUseCase queryUseCase;

    @Test
    @DisplayName("포트폴리오 조회 API")
    void getPortfoliosByUser_Success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        PortfolioGroupDto.PortfolioGroupResponse response1 = PortfolioGroupDto.PortfolioGroupResponse.builder()
                .id(1L)
                .name("공격형 투자")
                .iconCode("ROCKET")
                .build();

        PortfolioGroupDto.PortfolioGroupResponse response2 = PortfolioGroupDto.PortfolioGroupResponse.builder()
                .id(2L)
                .name("안전형 투자")
                .iconCode("SHIELD")
                .build();

        List<PortfolioGroupDto.PortfolioGroupResponse> mockResponseList = List.of(response1, response2);

        given(queryUseCase.getPortfoliosByUser(userId)).willReturn(mockResponseList);

        // when & then
        mockMvc.perform(get("/portfolios")
                        .header("Authorization", bearerToken(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("공격형 투자"))
                .andExpect(jsonPath("$[1].iconCode").value("SHIELD"));
    }

    @Test
    @DisplayName("포트폴리오 생성 성공 API")
    void createPortfolioGroup_Success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        PortfolioGroupDto.CreatePortfolioGroupRequest requestDto = PortfolioGroupDto.CreatePortfolioGroupRequest.builder()
                .name("새 포트폴리오")
                .iconCode("NEW_ICON")
                .build();

        // when & then
        mockMvc.perform(post("/portfolios")
                        .header("Authorization", bearerToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated());

        verify(commandUseCase).createPortfolioGroup(any(PortfolioGroupDto.CreatePortfolioGroupRequest.class), eq(userId));
    }

    @Test
    @DisplayName("포트폴리오 업데이트 성공 API")
    void updatePortfolioGroup_Success() throws Exception {
        // given
        Long portfolioGroupId = 1L;
        UUID userId = UUID.randomUUID();
        PortfolioGroupDto.UpdatePortfolioGroupRequest requestDto = PortfolioGroupDto.UpdatePortfolioGroupRequest.builder()
                .name("수정된 이름")
                .iconCode("EDIT_ICON")
                .build();

        // when & then
        mockMvc.perform(patch("/portfolios/{portfolioGroupId}", portfolioGroupId)
                        .header("Authorization", bearerToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(commandUseCase).updatePortfolioGroup(eq(portfolioGroupId), any(PortfolioGroupDto.UpdatePortfolioGroupRequest.class), eq(userId));
    }

    @Test
    @DisplayName("포트폴리오 삭제 성공 API")
    void deletePortfolioGroup_Success() throws Exception {
        // given
        Long portfolioGroupId = 1L;
        UUID userId = UUID.randomUUID();

        // when & then
        mockMvc.perform(delete("/portfolios/{portfolioGroupId}", portfolioGroupId)
                        .header("Authorization", bearerToken(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNoContent());

        // verify
        verify(commandUseCase).deletePortfolioGroup(portfolioGroupId, userId);
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
