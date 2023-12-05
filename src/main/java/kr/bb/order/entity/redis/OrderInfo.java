package kr.bb.order.entity.redis;

import java.util.List;
import javax.persistence.Id;
import kr.bb.order.dto.request.orderForDelivery.OrderForDeliveryRequest;
import kr.bb.order.dto.request.orderForDelivery.OrderInfoByStore;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.entity.delivery.OrderDeliveryStatus;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Builder
@RedisHash(value = "orderInfo", timeToLive = 300)
public class OrderInfo {
  @Id private String tempOrderId;
  private String itemName;
  private Long sumOfAllQuantity;
  private List<OrderInfoByStore> orderInfoByStores;
  private Long sumOfActualAmount;
  private boolean isSubscriptionPay;
  private String ordererName;
  private String ordererPhoneNumber;
  private String ordererEmail;
  private String recipientName;
  private String deliveryZipcode;
  private String deliveryRoadName;
  private String deliveryAddressDetail;
  private String recipientPhone;
  private String deliveryRequest;
  private String tid;

  public static OrderInfo transformDataForApi(
      String tempOrderId,
      String itemName,
      int quantity,
      String tid,
      OrderForDeliveryRequest requestDto) {

    return OrderInfo.builder()
        .tempOrderId(tempOrderId)
        .itemName(itemName)
        .sumOfAllQuantity((long) quantity)
        .orderInfoByStores(requestDto.getOrderInfoByStores())
        .isSubscriptionPay(requestDto.isSubscriptionPay())
        .ordererName(requestDto.getOrdererName())
        .ordererPhoneNumber(requestDto.getOrdererPhoneNumber())
        .ordererEmail(requestDto.getOrdererEmail())
        .recipientName(requestDto.getRecipientName())
        .deliveryZipcode(requestDto.getDeliveryZipcode())
        .deliveryRoadName(requestDto.getDeliveryRoadName())
        .deliveryAddressDetail(requestDto.getDeliveryAddressDetail())
        .ordererPhoneNumber(requestDto.getOrdererPhoneNumber())
        .deliveryRequest(requestDto.getDeliveryRequest())
        .tid(tid)
        .build();
  }
}
