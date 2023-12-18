package kr.bb.order.entity;

public enum OrderType {
  ORDER_DELIVERY("ORDER_DELIVERY"),
  ORDER_PICKUP("ORDER_PICKUP"),
  ORDER_SUBSCRIPTION("ORDER_SUBSCRIPTION"),
  ORDER_CART("ORDER_CART");

  private final String message;

  OrderType(String message) {
    this.message = message;
  }
}
