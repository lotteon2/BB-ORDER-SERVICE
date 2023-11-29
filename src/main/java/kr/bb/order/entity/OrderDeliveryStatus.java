package kr.bb.order.entity;

public enum OrderDeliveryStatus {
  PENDING("주문접수"),
  PROCESSING("배송시작"),
  COMPLETED("배송완료");

  private final String message;

  OrderDeliveryStatus(String message) {
    this.message = message;
  }
}
