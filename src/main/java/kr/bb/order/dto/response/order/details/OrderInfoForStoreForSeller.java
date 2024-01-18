package kr.bb.order.dto.response.order.details;

import java.util.List;
import java.util.Map;
import bloomingblooms.domain.delivery.DeliveryInfoDto;
import kr.bb.order.entity.delivery.OrderDelivery;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderInfoForStoreForSeller {
  private String orderGroupId;
  private String orderDeliveryId;
  private String storeName;
  private List<ProductRead> products;
  private String paymentDate;
  private String orderDeliveryStatus;
  private Long totalAmount;
  private Long deliveryCost;
  private Long couponAmount;
  private Long paymentAmount;
  private String ordererName;
  private String ordererPhoneNumber;
  private String ordererEmail;
  private String recipientName;
  private String zipcode;
  private String roadName;
  private String addressDetail;
  private String recipientPhoneNumber;
  private String deliveryRequest;

  public static OrderInfoForStoreForSeller toDto(
      OrderDelivery orderDelivery,
      List<ProductRead> productReadList,
      Map<Long, String> storeNameMap,
      String paymentDate,
      Map<Long, DeliveryInfoDto> deliveryInfoMap) {
    Long paymentAmount =
        orderDelivery.getOrderDeliveryTotalAmount()
            + deliveryInfoMap.get(orderDelivery.getDeliveryId()).getDeliveryCost()
            - orderDelivery.getOrderDeliveryCouponAmount();
    DeliveryInfoDto deliveryInfoDto = deliveryInfoMap.get(orderDelivery.getDeliveryId());

    return OrderInfoForStoreForSeller.builder()
        .orderGroupId(orderDelivery.getOrderGroup().getOrderGroupId())
        .orderDeliveryId(orderDelivery.getOrderDeliveryId())
        .storeName(storeNameMap.get(orderDelivery.getStoreId()))
        .products(productReadList)
        .paymentDate(paymentDate)
        .orderDeliveryStatus(orderDelivery.getOrderDeliveryStatus().toString())
        .totalAmount(orderDelivery.getOrderDeliveryTotalAmount())
        .deliveryCost(deliveryInfoDto.getDeliveryCost())
        .couponAmount(orderDelivery.getOrderDeliveryCouponAmount())
        .paymentAmount(paymentAmount)
        .ordererName(deliveryInfoDto.getOrdererName())
        .ordererPhoneNumber(deliveryInfoDto.getOrdererPhone())
        .ordererEmail(deliveryInfoDto.getOrdererEmail())
        .recipientName(deliveryInfoDto.getRecipientName())
        .zipcode(deliveryInfoDto.getZipcode())
        .roadName(deliveryInfoDto.getRoadName())
        .addressDetail(deliveryInfoDto.getAddressDetail())
        .recipientPhoneNumber(deliveryInfoDto.getRecipientPhone())
        .deliveryRequest(deliveryInfoDto.getDeliveryRequest())
        .build();
  }
}
