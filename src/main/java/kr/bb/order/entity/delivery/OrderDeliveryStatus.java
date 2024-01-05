package kr.bb.order.entity.delivery;

import lombok.Getter;

@Getter
public enum OrderDeliveryStatus {
  PENDING("주문접수"),
  PROCESSING("배송시작"),
  COMPLETED("배송완료");

  private final String message;

  OrderDeliveryStatus(String message) {
    this.message = message;
  }
}
