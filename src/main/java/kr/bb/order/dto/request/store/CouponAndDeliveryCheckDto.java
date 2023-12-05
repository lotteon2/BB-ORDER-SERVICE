package kr.bb.order.dto.request.store;

import kr.bb.order.dto.request.orderForDelivery.OrderInfoByStore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CouponAndDeliveryCheckDto {
  private Long couponId;
  private Long couponAmount;
  private Long deliveryCost;
  private Long actualAmount;

  public static CouponAndDeliveryCheckDto toDto(OrderInfoByStore orderInfoByStore) {
    return CouponAndDeliveryCheckDto.builder()
        .couponId(orderInfoByStore.getCouponId())
        .couponAmount(orderInfoByStore.getCouponAmount())
        .deliveryCost(orderInfoByStore.getDeliveryCost())
        .actualAmount(orderInfoByStore.getActualAmount())
        .build();
  }
}
