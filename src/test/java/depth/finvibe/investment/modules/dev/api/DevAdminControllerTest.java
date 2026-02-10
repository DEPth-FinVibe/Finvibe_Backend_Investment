package depth.finvibe.investment.modules.dev.api;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import depth.finvibe.investment.boot.config.WebMvcConfig;
import depth.finvibe.investment.boot.security.model.UserRole;
import depth.finvibe.investment.boot.security.resolver.JwtArgumentResolver;
import depth.finvibe.investment.modules.market.application.BatchPriceUpdateService;
import depth.finvibe.investment.modules.market.infra.client.tokenmanage.repository.TokenRepository;
import depth.finvibe.investment.shared.infra.error.GlobalErrorHttpMapper;
import depth.finvibe.investment.shared.infra.error.GlobalExceptionHandler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DevAdminController.class)
@Import({
    WebMvcConfig.class,
    JwtArgumentResolver.class,
    GlobalExceptionHandler.class,
    GlobalErrorHttpMapper.class
})
class DevAdminControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private TokenRepository tokenRepository;

  @MockitoBean
  private BatchPriceUpdateService batchPriceUpdateService;

  @Test
  @DisplayName("관리자 권한으로 배치 가격 업데이트 강제 실행 API 호출 시 성공한다")
  void runBatchPriceUpdate_admin_success() throws Exception {
    UUID userId = UUID.randomUUID();

    mockMvc.perform(post("/dev/market/batch-price-update")
            .header("Authorization", bearerToken(userId, UserRole.ADMIN))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    verify(batchPriceUpdateService).updateHoldingStockPrices();
  }

  @Test
  @DisplayName("일반 사용자 권한으로 배치 가격 업데이트 강제 실행 API 호출 시 접근이 거부된다")
  void runBatchPriceUpdate_user_forbidden() throws Exception {
    UUID userId = UUID.randomUUID();

    mockMvc.perform(post("/dev/market/batch-price-update")
            .header("Authorization", bearerToken(userId, UserRole.USER))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    verifyNoInteractions(batchPriceUpdateService);
  }

  private String bearerToken(UUID userId, UserRole role) throws Exception {
    String header = Base64.getUrlEncoder().withoutPadding()
        .encodeToString("{}".getBytes(StandardCharsets.UTF_8));
    String payload = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(objectMapper.writeValueAsBytes(Map.of(
            "id", userId.toString(),
            "role", role.name()
        )));
    return "Bearer " + header + "." + payload + ".sig";
  }
}
