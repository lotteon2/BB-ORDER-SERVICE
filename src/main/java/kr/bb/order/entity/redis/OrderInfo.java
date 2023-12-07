package kr.bb.order.entity.redis;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.persistence.Id;
import kr.bb.order.dto.request.orderForDelivery.OrderForDeliveryRequest;
import kr.bb.order.dto.request.orderForDelivery.OrderInfoByStore;
import kr.bb.order.entity.common.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@RedisHash(value = "orderInfo", timeToLive = 300)
public class OrderInfo extends BaseEntity {
  @Id private String tempOrderId;
  private Long userId;
  private String itemName;
  private Long sumOfAllQuantity;
  private List<OrderInfoByStore> orderInfoByStores;
  private Long sumOfActualAmount;

  @JsonProperty(value="subscriptionPay")
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
  private String pgToken;

  public static OrderInfo transformDataForApi(
      String tempOrderId,
      Long userId,
      String itemName,
      int quantity,
      boolean isSubscriptionPay,
      String tid,
      OrderForDeliveryRequest requestDto) {

    return OrderInfo.builder()
        .tempOrderId(tempOrderId)
        .userId(userId)
        .itemName(itemName)
        .sumOfAllQuantity((long) quantity)
        .orderInfoByStores(requestDto.getOrderInfoByStores())
        .sumOfActualAmount(requestDto.getSumOfActualAmount())
        .isSubscriptionPay(isSubscriptionPay)
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

  public void setPgToken(String pgToken) {
    this.pgToken = pgToken;
  }
}
