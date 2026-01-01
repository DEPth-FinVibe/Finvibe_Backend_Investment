package depth.finvibe.investment.shared.infra.error;

import depth.finvibe.investment.shared.error.DomainErrorCode;
import depth.finvibe.investment.shared.error.DomainException;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final List<DomainErrorHttpMapper> mappers;

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex) {
    HttpStatusCode status = resolveStatus(ex.getErrorCode());

    ErrorResponse body = ErrorResponse.of(
        ex.getErrorCode().getCode(),
        ex.getErrorCode().getMessageKey()
    );
    return ResponseEntity.status(status).body(body);
  }

  private HttpStatusCode resolveStatus(DomainErrorCode code) {
    return mappers.stream()
        .filter(mapper -> mapper.supports(code))
        .findFirst()
        .map(mapper -> mapper.toStatus(code))
        .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
