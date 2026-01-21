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
     * userId는 랜덤 생성, role은 USER, 만료기한은 1일(86400초)로 고정됩니다.
     *
     * @return 생성된 JWT 토큰
     */
    @PostMapping("/jwt/token")
    public ResponseEntity<JwtTokenResponse> generateToken() {
        UUID userId = UUID.randomUUID();
        UserRole role = UserRole.USER;
        Long expirationSeconds = 86400L; // 1일
        
        String token = jwtTokenGenerator.generate(userId, role, expirationSeconds);

        return ResponseEntity.ok(new JwtTokenResponse(token, userId, role));
    }

    /**
     * JWT 토큰 생성 응답
     */
    public record JwtTokenResponse(
            String token,
            UUID userId,
            UserRole role
    ) {}
}
