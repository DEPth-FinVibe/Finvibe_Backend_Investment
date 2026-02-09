package depth.finvibe.investment.modules.dev.api;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import depth.finvibe.investment.boot.security.model.AuthenticatedUser;
import depth.finvibe.investment.boot.security.model.Requester;
import depth.finvibe.investment.boot.security.model.UserRole;
import depth.finvibe.investment.modules.market.infra.client.tokenmanage.repository.TokenRepository;
import depth.finvibe.investment.shared.error.DomainException;
import depth.finvibe.investment.shared.error.GlobalErrorCode;

@Hidden
@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
public class DevAdminController {

  private final TokenRepository tokenRepository;

  @GetMapping("/kis/token")
  @Operation(summary = "KIS 토큰 조회", description = "appKey로 Redis에 저장된 KIS access token을 조회합니다.")
  public ResponseEntity<KisTokenResponse> getKisToken(
      @Parameter(description = "KIS appKey", example = "appKey1234") @RequestParam String appKey,
      @Parameter(hidden = true) @AuthenticatedUser Requester requester
  ) {
    ensureAdmin(requester);

    String token = tokenRepository.getAccessToken(appKey);
    LocalDateTime expiresAt = tokenRepository.getExpiresAt(appKey);

    return ResponseEntity.ok(new KisTokenResponse(appKey, token, expiresAt, token != null));
  }

  private void ensureAdmin(Requester requester) {
    if (requester.getRole() != UserRole.ADMIN) {
      throw new DomainException(GlobalErrorCode.ACCESS_DENIED);
    }
  }

  public record KisTokenResponse(
      String appKey,
      String token,
      LocalDateTime expiresAt,
      boolean exists
  ) {
  }
}
