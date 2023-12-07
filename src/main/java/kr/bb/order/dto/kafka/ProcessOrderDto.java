package kr.bb.order.dto.kafka;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import kr.bb.order.dto.request.orderForDelivery.OrderInfoByStore;
import kr.bb.order.dto.request.orderForDelivery.ProductCreate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessOrderDto {
  private String orderGroupId;
  private List<Long> couponIds;
  private List<Long> productIds;

  public static ProcessOrderDto toDto(String orderGroupId, List<OrderInfoByStore> orderInfoByStores){

    List<Long> couponIds = orderInfoByStores.stream().map(OrderInfoByStore::getCouponId).collect(
            Collectors.toList());
    List<Long> productIds = orderInfoByStores.stream().flatMap(orderInfoByStore -> orderInfoByStore.getProducts().stream()).map(ProductCreate::getProductId).collect(
            Collectors.toList());

    return ProcessOrderDto.builder()
            .orderGroupId(orderGroupId)
            .couponIds(couponIds)
            .productIds(productIds)
            .build();
  }
}
