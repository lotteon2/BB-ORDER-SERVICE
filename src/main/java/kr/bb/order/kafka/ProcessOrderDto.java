package kr.bb.order.kafka;

import java.util.List;
import java.util.Map;
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
  private Map<String, Long> products; // productId: quantity

  public static ProcessOrderDto toDtoForOrderDelivery(
      String orderId, String orderType, List<OrderInfoByStore> orderInfoByStores) {

    List<Long> couponIds =
        orderInfoByStores.stream().map(OrderInfoByStore::getCouponId).collect(Collectors.toList());
    Map<String, Long> products =
        orderInfoByStores.stream()
            .flatMap(orderInfoByStore -> orderInfoByStore.getProducts().stream())
            .collect(Collectors.toMap(ProductCreate::getProductId, ProductCreate::getQuantity));

    return ProcessOrderDto.builder()
        .orderId(orderId)
        .orderType(orderType)
        .couponIds(couponIds)
        .products(products)
        .build();
  }

  public static ProcessOrderDto toDtoForOrderPickup(
      String orderId, PickupOrderInfo pickupOrderInfo) {
    List<Long> couponIds = List.of(pickupOrderInfo.getCouponId());
    Map<String, Long> product =
        Map.of(
            pickupOrderInfo.getProduct().getProductId(),
            pickupOrderInfo.getProduct().getQuantity());

    return ProcessOrderDto.builder()
        .orderId(orderId)
        .orderType(pickupOrderInfo.getOrderType())
        .couponIds(couponIds)
        .products(product)
        .build();
  }
}
