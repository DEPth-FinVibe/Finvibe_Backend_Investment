package depth.finvibe.investment.modules.asset.api;

import depth.finvibe.investment.modules.asset.application.port.in.AssetQueryUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssetController.class)
public class AssetControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssetQueryUseCase assetQueryUseCase; // 가짜 객체 주입

    @Test
    @DisplayName("포트폴리오 자산 조회 API - userId 파라미터 수신 확인")
    void getAssetsByPortfolio() throws Exception {
        // given
        String testUuid = UUID.randomUUID().toString();

        // when & then
        mockMvc.perform(get("/asset/portfolio")
                        .param("portfolioId", "1")
                        .param("userId", testUuid))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("포트폴리오 자산 조회 API - userId 파라미터 수신 확인")
    void getPortfoliosByUser() throws Exception {
        // given
        String testUuid = UUID.randomUUID().toString();

        // when & then
        mockMvc.perform(get("/asset/user")
                        .param("userId", testUuid))
                .andExpect(status().isOk());
    }
}
