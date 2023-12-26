package kr.bb.order.mapper;

import bloomingblooms.domain.payment.KakaopayApproveRequestDto;
import kr.bb.order.entity.redis.OrderInfo;
import kr.bb.order.entity.redis.PickupOrderInfo;

public class KakaopayMapper {
  public static KakaopayApproveRequestDto toDtoFromOrderInfo(OrderInfo orderInfo) {
    return KakaopayApproveRequestDto.builder()
        .userId(orderInfo.getUserId())
        .orderId(orderInfo.getTempOrderId())
        .orderType(orderInfo.getOrderType())
        .itemName(orderInfo.getItemName())
        .quantity(Math.toIntExact(orderInfo.getSumOfAllQuantity()))
        .totalAmount(Math.toIntExact(orderInfo.getSumOfActualAmount()))
        .taxFreeAMount(0)
        .isSubscriptionPay(orderInfo.isSubscriptionPay())
        .tid(orderInfo.getTid())
        .pgToken(orderInfo.getPgToken())
        .build();
  }

  public static KakaopayApproveRequestDto toDtoFromPickupOrderInfo(
      PickupOrderInfo pickupOrderInfo) {
    return KakaopayApproveRequestDto.builder()
        .userId(pickupOrderInfo.getUserId())
        .orderId(pickupOrderInfo.getTempOrderId())
        .orderType(pickupOrderInfo.getOrderType())
        .itemName(pickupOrderInfo.getItemName())
        .quantity(Math.toIntExact(pickupOrderInfo.getQuantity()))
        .totalAmount(Math.toIntExact(pickupOrderInfo.getTotalAmount()))
        .taxFreeAMount(0)
        .isSubscriptionPay(pickupOrderInfo.isSubscriptionPay())
        .tid(pickupOrderInfo.getTid())
        .pgToken(pickupOrderInfo.getPgToken())
        .build();
  }
}
