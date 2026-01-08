package depth.finvibe.investment.boot.security.resolver;

import depth.finvibe.investment.boot.security.model.AuthenticatedUser;
import depth.finvibe.investment.boot.security.model.Requester;
import depth.finvibe.investment.boot.security.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtArgumentResolverTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtArgumentResolver resolver = new JwtArgumentResolver(objectMapper);

    @Test
    @DisplayName("supportsParameter: @AuthenticatedUser + Requester 조합만 지원한다")
    void supportsParameter() throws Exception {
        MethodParameter annotatedRequester = methodParam("handler", Requester.class);
        MethodParameter nonAnnotatedRequester = methodParam("handlerWithoutAnnotation", Requester.class);
        MethodParameter annotatedWrongType = methodParam("handlerWrongType", String.class);

        assertThat(resolver.supportsParameter(annotatedRequester)).isTrue();
        assertThat(resolver.supportsParameter(nonAnnotatedRequester)).isFalse();
        assertThat(resolver.supportsParameter(annotatedWrongType)).isFalse();
    }

    @Test
    @DisplayName("resolveArgument: 유효한 토큰이면 Requester를 반환한다")
    void resolveArgumentWithValidToken() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtToken(Map.of(
                "id", userId.toString(),
                "role", UserRole.USER.name()
        ));
        NativeWebRequest webRequest = webRequestWithToken(token);

        Object resolved = resolver.resolveArgument(
                methodParam("handler", Requester.class),
                null,
                webRequest,
                null
        );

        assertThat(resolved).isInstanceOf(Requester.class);
        Requester requester = (Requester) resolved;
        assertThat(requester.getUuid()).isEqualTo(userId);
        assertThat(requester.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("resolveArgument: Authorization 헤더가 없으면 null을 반환한다")
    void resolveArgumentWithoutAuthorizationHeader() throws Exception {
        NativeWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest());

        Object resolved = resolver.resolveArgument(
                methodParam("handler", Requester.class),
                null,
                webRequest,
                null
        );

        assertThat(resolved).isNull();
    }

    @Test
    @DisplayName("resolveArgument: 형식이 잘못된 토큰이면 null을 반환한다")
    void resolveArgumentWithInvalidToken() throws Exception {
        NativeWebRequest webRequest = webRequestWithToken("not-a-jwt");

        Object resolved = resolver.resolveArgument(
                methodParam("handler", Requester.class),
                null,
                webRequest,
                null
        );

        assertThat(resolved).isNull();
    }

    private static MethodParameter methodParam(String methodName, Class<?> parameterType) throws Exception {
        return new MethodParameter(TestController.class.getMethod(methodName, parameterType), 0);
    }

    private NativeWebRequest webRequestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return new ServletWebRequest(request);
    }

    private String jwtToken(Map<String, Object> claims) throws Exception {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(claims));
        return header + "." + payload + ".sig";
    }

    private static class TestController {
        public void handler(@AuthenticatedUser Requester requester) {
        }

        public void handlerWithoutAnnotation(Requester requester) {
        }

        public void handlerWrongType(@AuthenticatedUser String requester) {
        }
    }
}
