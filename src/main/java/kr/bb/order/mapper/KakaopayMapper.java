package kr.bb.order.dto.request.payment;

import bloomingblooms.domain.payment.KakaopayApproveRequestDto;
import kr.bb.order.entity.redis.OrderInfo;

public class KakaopayApproveRequestDtoManager {
  public static KakaopayApproveRequestDto toDto(OrderInfo orderInfo, String orderType) {
    return KakaopayApproveRequestDto.builder()
        .userId(orderInfo.getUserId())
        .orderId(orderInfo.getTempOrderId())
        .orderType(orderType)
        .itemName(orderInfo.getItemName())
        .quantity(Math.toIntExact(orderInfo.getSumOfAllQuantity()))
        .totalAmount(Math.toIntExact(orderInfo.getSumOfActualAmount()))
        .taxFreeAMount(0)
        .isSubscriptionPay(orderInfo.isSubscriptionPay())
        .tid(orderInfo.getTid())
        .pgToken(orderInfo.getPgToken())
        .build();
  }
}
