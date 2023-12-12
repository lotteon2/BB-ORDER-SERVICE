package kr.bb.order.exception;

import kr.bb.order.exception.common.ErrorCode;

public class OrderGroupEntityNotFoundException extends RuntimeException {
  private static final String message = ErrorCode.ORDER_DELIVERY_NOT_FOUND_EXCEPTION.getMessage();

  public OrderGroupEntityNotFoundException() {
    super(message);
  }
}
