package kr.bb.order.dto.response.order.details;

import java.util.List;
import bloomingblooms.domain.delivery.DeliveryInfoDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDeliveryGroup {
  private String orderGroupId;
  private List<OrderInfoForStore> orderDeliveries;
  private String paymentDate;
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

  public static OrderDeliveryGroup toDto(String orderGroupId, List<OrderInfoForStore> orderInfoForStores, String paymentDate, DeliveryInfoDto deliveryInfoDto) {
    Long totalAmount = orderInfoForStores.stream().mapToLong(OrderInfoForStore::getTotalAmount).sum();
    Long deliveryCost = orderInfoForStores.stream().mapToLong(OrderInfoForStore::getDeliveryCost).sum();
    Long couponAmount = orderInfoForStores.stream().mapToLong(OrderInfoForStore::getCouponAmount).sum();
    Long paymentAmount = orderInfoForStores.stream().mapToLong(OrderInfoForStore::getPaymentAmount).sum();

   return OrderDeliveryGroup.builder()
        .orderGroupId(orderGroupId)
        .orderDeliveries(orderInfoForStores)
        .paymentDate(paymentDate)
        .totalAmount(totalAmount)
        .deliveryCost(deliveryCost)
        .couponAmount(couponAmount)
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
