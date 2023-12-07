package kr.bb.order.exception;

import kr.bb.order.exception.common.ErrorCode;

public class OrderDeliveryEntityNotFoundException extends RuntimeException {
  private static final String message = ErrorCode.ORDER_DELIVERY_NOT_FOUND_EXCEPTION.getMessage();

  public OrderDeliveryEntityNotFoundException() {
    super(message);
  }
}
