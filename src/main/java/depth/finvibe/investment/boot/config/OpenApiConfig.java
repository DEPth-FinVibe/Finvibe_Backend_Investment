package depth.finvibe.investment.boot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import depth.finvibe.investment.boot.security.model.AuthenticatedUser;

@Configuration
public class OpenApiConfig {

    static {
        SpringDocUtils.getConfig()
                .addAnnotationsToIgnore(AuthenticatedUser.class);
    }

    @Bean
    public OpenAPI finvibeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Finvibe Investment API")
                        .description("Finvibe Investment 서비스 API 문서")
                        .version("v1"));
    }

    @Bean
    public GroupedOpenApi assetApi() {
        return GroupedOpenApi.builder()
                .group("asset")
                .pathsToMatch("/portfolios/**")
                .build();
    }

    @Bean
    public GroupedOpenApi walletApi() {
        return GroupedOpenApi.builder()
                .group("wallet")
                .pathsToMatch("/wallets/**")
                .build();
    }

    @Bean
    public GroupedOpenApi tradeApi() {
        return GroupedOpenApi.builder()
                .group("trade")
                .pathsToMatch("/trades/**")
                .build();
    }

    @Bean
    public GroupedOpenApi marketApi() {
        return GroupedOpenApi.builder()
                .group("market")
                .pathsToMatch("/market/**")
                .build();
    }
}
