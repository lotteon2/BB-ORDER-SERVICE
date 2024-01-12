package kr.bb.order.entity.pickup;

import lombok.Getter;

@Getter
public enum OrderPickupStatus {
  PENDING("주문접수"),
  COMPLETED("픽업완료"),
  CANCELED("주문취소");

  private final String message;

  OrderPickupStatus(String message) {
    this.message = message;
  }
}
