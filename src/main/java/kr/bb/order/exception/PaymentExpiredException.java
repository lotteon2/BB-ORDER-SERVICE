package kr.bb.order.exception;

import kr.bb.order.exception.common.ErrorCode;

public class PaymentExpiredException extends RuntimeException {
  private static final String message = ErrorCode.PAYMENT_EXPIRED_EXCEPTION.getMessage();

  public PaymentExpiredException() {
    super(message);
  }
}
