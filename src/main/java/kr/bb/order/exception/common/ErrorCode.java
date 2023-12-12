package kr.bb.order.exception.common;

import javax.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  INVALID_ORDER_ACTUAL_AMOUNT(HttpStatus.BAD_REQUEST, "유효하지 않은 주문 금액입니다"),
  ORDER_DELIVERY_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, "주문 정보를 찾을 수 없습니다."),
  PAYMENT_EXPIRED_EXCEPTION(HttpStatus.NOT_FOUND, "결제 시간이 만료되었습니다. 다시 시도해주세요."),
  PAYMENT_FEIGN_CLIENT_EXCEPTION(HttpStatus.BAD_REQUEST, "결제 서비스 응답이 잘못되었습니다.");

  private final HttpStatus code;
  private final String message;

  ErrorCode(HttpStatus code, String message) {
    this.code = code;
    this.message = message;
  }
}
