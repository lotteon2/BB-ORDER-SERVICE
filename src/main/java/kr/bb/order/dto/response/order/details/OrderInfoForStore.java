package kr.bb.order.dto.response.order.details;

import bloomingblooms.domain.delivery.DeliveryInfoDto;
import java.util.List;
import java.util.Map;
import kr.bb.order.entity.delivery.OrderDelivery;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderInfoForStore {
  private String orderDeliveryId;
  private Long storeId;
  private String storeName;
  private List<ProductRead> products;
  private String orderDeliveryStatus;
  private Long totalAmount;
  private Long deliveryCost;
  private Long couponAmount;
  private Long paymentAmount;

  public static OrderInfoForStore toDto(
      OrderDelivery orderDelivery,
      List<ProductRead> productReadList,
      Map<Long, String> storeNameMap,
      Map<Long, DeliveryInfoDto> deliveryInfoMap) {
    Long storeId = orderDelivery.getStoreId();
    return OrderInfoForStore.builder()
        .orderDeliveryId(orderDelivery.getOrderDeliveryId())
        .storeId(storeId)
        .storeName(storeNameMap.get(storeId))
        .products(productReadList)
        .orderDeliveryStatus(orderDelivery.getOrderDeliveryStatus().toString())
        .totalAmount(orderDelivery.getOrderDeliveryTotalAmount())
        .deliveryCost(deliveryInfoMap.get(orderDelivery.getDeliveryId()).getDeliveryCost())
        .couponAmount(orderDelivery.getOrderDeliveryCouponAmount())
        .paymentAmount(
            orderDelivery.getOrderDeliveryTotalAmount()
                + deliveryInfoMap.get(orderDelivery.getDeliveryId()).getDeliveryCost()
                - orderDelivery.getOrderDeliveryCouponAmount())
        .build();
  }
}
