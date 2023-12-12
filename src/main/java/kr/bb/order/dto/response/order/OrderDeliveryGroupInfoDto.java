package kr.bb.order.dto.response.order;

import java.util.List;
import kr.bb.order.dto.request.payment.PaymentInfoDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDeliveryGroupInfoDto {
  private String orderGroupId;
  private List<OrderDeliveryInfoDto> orderDeliveryInfoDtoList;
  private Long paymentAmount;
  private String paymentDate;
  public static OrderDeliveryGroupInfoDto toDto(
      String orderGroupId, List<OrderDeliveryInfoDto> orderDeliveryInfoDtos,
      PaymentInfoDto paymentInfoDto) {

      return OrderDeliveryGroupInfoDto.builder()
              .orderGroupId(orderGroupId)
              .orderDeliveryInfoDtoList(orderDeliveryInfoDtos)
              .paymentAmount(paymentInfoDto.getPaymentActualAmount())
              .paymentDate(paymentInfoDto.getCreatedAt().toString())
              .build();
  }
}
