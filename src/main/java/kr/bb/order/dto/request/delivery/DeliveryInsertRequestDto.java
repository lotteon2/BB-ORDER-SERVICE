package kr.bb.order.dto.request.delivery;

import java.util.ArrayList;
import java.util.List;
import kr.bb.order.dto.request.orderForDelivery.OrderInfoByStore;
import kr.bb.order.entity.redis.OrderInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryInsertRequestDto {
  private String ordererName;
  private String ordererPhoneNumber;
  private String ordererEmail;
  private String recipientName;
  private String recipientPhoneNumber;
  private String zipcode;
  private String roadName;
  private String addressDetail;
  private String request;
  private Long deliveryCost;

  public static List<DeliveryInsertRequestDto> toDto(OrderInfo orderInfo){

    // 가게 개수 만큼 배송 정보 생성
    List<OrderInfoByStore> orderInfoByStores = orderInfo.getOrderInfoByStores();

    List<DeliveryInsertRequestDto> list = new ArrayList<>();
    for(int i=0; i<orderInfoByStores.size(); i++){
      DeliveryInsertRequestDto dto = DeliveryInsertRequestDto.builder()
              .ordererName(orderInfo.getOrdererName())
              .ordererPhoneNumber(orderInfo.getOrdererPhoneNumber())
              .ordererEmail(orderInfo.getOrdererEmail())
              .recipientName(orderInfo.getRecipientName())
              .recipientPhoneNumber(orderInfo.getRecipientPhone())
              .zipcode(orderInfo.getDeliveryZipcode())
              .roadName(orderInfo.getDeliveryRoadName())
              .addressDetail(orderInfo.getDeliveryAddressDetail())
              .request(orderInfo.getDeliveryRequest())
              .deliveryCost(orderInfoByStores.get(i).getDeliveryCost())
              .build();
      list.add(dto);
    }

    return list;
  }
}
