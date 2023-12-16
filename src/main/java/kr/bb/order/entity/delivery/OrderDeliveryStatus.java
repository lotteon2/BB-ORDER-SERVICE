package kr.bb.order.entity.delivery;

public enum OrderDeliveryStatus {
  INITIAL("주문 접수전"),
  PENDING("주문접수"),
  PROCESSING("배송시작"),
  COMPLETED("배송완료");

  private final String message;

  OrderDeliveryStatus(String message) {
    this.message = message;
  }
}
