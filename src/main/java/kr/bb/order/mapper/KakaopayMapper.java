package kr.bb.order.mapper;

import bloomingblooms.domain.payment.KakaopayApproveRequestDto;
import java.util.List;
import kr.bb.order.entity.redis.OrderInfo;
import kr.bb.order.entity.redis.PickupOrderInfo;
import kr.bb.order.entity.redis.SubscriptionOrderInfo;

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
        .cid("TC0ONETIME")
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
        .cid("TC0ONETIME")
        .pgToken(pickupOrderInfo.getPgToken())
        .build();
  }

  public static KakaopayApproveRequestDto toDtoFromSubscriptionOrderInfo(
      SubscriptionOrderInfo subscriptionOrderInfo, List<Long> deliveryIds) {
    return KakaopayApproveRequestDto.builder()
        .userId(subscriptionOrderInfo.getUserId())
        .orderId(subscriptionOrderInfo.getTempOrderId())
        .orderType(subscriptionOrderInfo.getOrderType())
        .itemName(subscriptionOrderInfo.getItemName())
        .quantity(Math.toIntExact(subscriptionOrderInfo.getQuantity()))
        .totalAmount(Math.toIntExact(subscriptionOrderInfo.getTotalAmount()))
        .taxFreeAMount(0)
        .isSubscriptionPay(subscriptionOrderInfo.isSubscriptionPay())
        .cid("TCSUBSCRIP")
        .tid(subscriptionOrderInfo.getTid())
        .pgToken(subscriptionOrderInfo.getPgToken())
        .deliveryId(deliveryIds.get(0))
        .build();
  }
}
