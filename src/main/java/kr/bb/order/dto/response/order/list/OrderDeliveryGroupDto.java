package kr.bb.order.dto.response.order.list;

import bloomingblooms.domain.payment.PaymentInfoDto;
import bloomingblooms.domain.product.ProductInformation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kr.bb.order.entity.delivery.OrderGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDeliveryGroupDto {
  private String key;
  private String orderStatus;
  private String thumbnailImage;
  private String productName;
  private Long quantity;
  private Long paymentAmount;
  private String paymentDate;
  private Long storeCount;

  public static List<OrderDeliveryGroupDto> toDto(
      List<OrderGroup> orderGroupsList,
      List<Long> storeCounts,
      List<String> productIds,
      Map<String, ProductInformation> productInfoDtoMap,
      List<PaymentInfoDto> paymentInfo) {
    List<OrderDeliveryGroupDto> orderDeliveryGroupDtos = new ArrayList<>();

    Map<String, PaymentInfoDto> paymentInfoDtoMap =
        paymentInfo.stream().collect(Collectors.toMap(PaymentInfoDto::getOrderGroupId, dto -> dto));

    for (int i = 0; i < orderGroupsList.size(); i++) {
      String key = orderGroupsList.get(i).getOrderGroupId();
      OrderDeliveryGroupDto orderDeliveryGroupDto =
          OrderDeliveryGroupDto.builder()
              .key(orderGroupsList.get(i).getOrderGroupId())
              // 한 가게의 주문상태가 해당 그룹주문의 상태가 된다.
              .orderStatus(
                  orderGroupsList
                      .get(i)
                      .getOrderDeliveryList()
                      .get(0)
                      .getOrderDeliveryStatus()
                      .toString())
              .thumbnailImage(productInfoDtoMap.get(productIds.get(i)).getProductThumbnail())
              .productName(productInfoDtoMap.get(productIds.get(i)).getProductName())
              .quantity(
                  orderGroupsList.get(i).getOrderDeliveryList().stream()
                      .mapToLong(orderDelivery -> orderDelivery.getOrderDeliveryProducts().size())
                      .sum())
              .paymentAmount(paymentInfoDtoMap.get(key).getPaymentActualAmount())
              .paymentDate(paymentInfoDtoMap.get(key).getCreatedAt().toLocalDate().toString())
              .storeCount(storeCounts.get(i))
              .build();

      orderDeliveryGroupDtos.add(orderDeliveryGroupDto);
    }
    return orderDeliveryGroupDtos;
  }
}
