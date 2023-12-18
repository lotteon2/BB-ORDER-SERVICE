package kr.bb.order.dto.request.store;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import kr.bb.order.dto.request.orderForDelivery.OrderInfoByStore;
import kr.bb.order.dto.request.orderForDelivery.ProductCreate;
import kr.bb.order.entity.redis.PickupOrderInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessOrderDto {
  private String orderId;
  private String orderType;
  private List<Long> couponIds;
  private List<String> productIds;

  public static ProcessOrderDto toDtoForOrderDelivery(String orderId, String orderType, List<OrderInfoByStore> orderInfoByStores){

    List<Long> couponIds = orderInfoByStores.stream().map(OrderInfoByStore::getCouponId).collect(
            Collectors.toList());
    List<String> productIds = orderInfoByStores.stream().flatMap(orderInfoByStore -> orderInfoByStore.getProducts().stream()).map(ProductCreate::getProductId).collect(
            Collectors.toList());

    return ProcessOrderDto.builder()
            .orderId(orderId)
            .orderType(orderType)
            .couponIds(couponIds)
            .productIds(productIds)
            .build();
  }

  public static ProcessOrderDto toDtoForOrderPickup(String orderId, PickupOrderInfo pickupOrderInfo ){
    List<Long> couponIds = new ArrayList<>();
    couponIds.add(pickupOrderInfo.getCouponId());
    List<String> productIds = new ArrayList<>();
    productIds.add(pickupOrderInfo.getProduct().getProductId());

    return ProcessOrderDto.builder()
            .orderId(orderId)
            .orderType(pickupOrderInfo.getOrderType())
            .couponIds(couponIds)
            .productIds(productIds)
            .build();
  }
}
