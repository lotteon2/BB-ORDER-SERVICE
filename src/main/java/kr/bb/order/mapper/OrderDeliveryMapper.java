package kr.bb.order.mapper;

import bloomingblooms.domain.delivery.DeliveryInsertDto;
import bloomingblooms.domain.order.OrderInfoByStore;
import java.util.ArrayList;
import java.util.List;
import kr.bb.order.entity.redis.OrderInfo;

public class OrderDeliveryMapper {
  public static List<DeliveryInsertDto> toDto(OrderInfo orderInfo) {
    List<OrderInfoByStore> orderInfoByStores = orderInfo.getOrderInfoByStores();
    List<DeliveryInsertDto> list = new ArrayList<>();

    for (OrderInfoByStore orderInfoByStore : orderInfoByStores) {
      DeliveryInsertDto dto =
          DeliveryInsertDto.builder()
              .ordererName(orderInfo.getOrdererName())
              .ordererPhoneNumber(orderInfo.getOrdererPhoneNumber())
              .ordererEmail(orderInfo.getOrdererEmail())
              .recipientName(orderInfo.getRecipientName())
              .recipientPhoneNumber(orderInfo.getRecipientPhone())
              .zipcode(orderInfo.getDeliveryZipcode())
              .roadName(orderInfo.getDeliveryRoadName())
              .addressDetail(orderInfo.getDeliveryAddressDetail())
              .request(orderInfo.getDeliveryRequest())
              .deliveryCost(orderInfoByStore.getDeliveryCost())
              .build();
      list.add(dto);
    }

    return list;
  }
}
