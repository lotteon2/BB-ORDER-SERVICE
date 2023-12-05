package kr.bb.order.entity;

public enum OrderType {
  ORDER_DELIVERY("ORDER_TYPE"),
  ORDER_PICKUP("ORDER_PICKUP"),
  ORDER_SUBSCRIPTION("ORDER_SUBSCRIPTION");

  private final String message;

  OrderType(String message) {
    this.message = message;
  }
}
