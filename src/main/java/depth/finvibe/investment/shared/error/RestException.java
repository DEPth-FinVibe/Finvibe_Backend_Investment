package depth.finvibe.investment.shared.error;

import org.springframework.http.HttpStatusCode;

import lombok.Getter;

/**
 * REST API 레이어에서 발생하는 예외를 표현하는 클래스입니다.
 * 
 * <p>
 * 도메인 레이어의 {@link DomainException}을 상속받아 비즈니스 에러 정보를 유지하면서,
 * 인프라스트럭처 레이어인 HTTP 통신에 필요한 상태 코드(Status Code)를 결합합니다.
 * 이 예외는 주로 ControllerAdvice 등에서 캡처되어 클라이언트에게 표준화된 JSON 응답으로 변환됩니다.
 * </p>
 */
@Getter
public class RestException extends DomainException {
  /**
   * RestException을 생성합니다.
   * 
   * @param errorCode      비즈니스 에러 상세 정보
   * @param httpStatusCode 클라이언트에게 응답할 HTTP 상태 코드
   */
  public RestException(DomainErrorCode errorCode, HttpStatusCode httpStatusCode) {
    super(errorCode);
    this.httpStatusCode = httpStatusCode;
  }

  /**
   * 해당 에러에 매핑되는 HTTP 응답 상태 코드입니다.
   */
  private final HttpStatusCode httpStatusCode;
}
