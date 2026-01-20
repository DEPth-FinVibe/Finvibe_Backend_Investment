package depth.finvibe.investment.modules.dev.api;

import depth.finvibe.investment.boot.security.JwtTokenGenerator;
import depth.finvibe.investment.boot.security.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 개발 환경 전용 테스트 API 컨트롤러
 * local 프로파일에서만 활성화됩니다.
 */
@Profile("local")
@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
public class DevController {

    private final JwtTokenGenerator jwtTokenGenerator;

    /**
     * WebSocket 인증용 테스트 JWT 토큰을 발급합니다.
     *
     * @param request 토큰 생성 요청
     * @return 생성된 JWT 토큰
     */
    @PostMapping("/jwt/token")
    public ResponseEntity<JwtTokenResponse> generateToken(@RequestBody(required = false) JwtTokenRequest request) {
        UUID userId = (request != null && request.userId != null) 
                ? request.userId 
                : UUID.randomUUID();
        
        UserRole role = (request != null && request.role != null) 
                ? request.role 
                : UserRole.USER;
        
        String token;
        if (request != null && request.expirationSeconds != null && request.expirationSeconds > 0) {
            token = jwtTokenGenerator.generate(userId, role, request.expirationSeconds);
        } else {
            // 기본값: 24시간
            token = jwtTokenGenerator.generate(userId, role, 86400L);
        }

        return ResponseEntity.ok(new JwtTokenResponse(token, userId, role));
    }

    /**
     * JWT 토큰 생성 요청
     */
    public record JwtTokenRequest(
            UUID userId,
            UserRole role,
            Long expirationSeconds
    ) {}

    /**
     * JWT 토큰 생성 응답
     */
    public record JwtTokenResponse(
            String token,
            UUID userId,
            UserRole role
    ) {}
}
