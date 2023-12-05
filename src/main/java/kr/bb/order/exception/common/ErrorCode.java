package kr.bb.order.exception.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  INVALID_ORDER_ACTUAL_AMOUNT(HttpStatus.BAD_REQUEST, "유효하지 않은 주문 금액입니다");

  private final HttpStatus code;
  private final String message;

  ErrorCode(HttpStatus code, String message) {
    this.code = code;
    this.message = message;
  }
}
