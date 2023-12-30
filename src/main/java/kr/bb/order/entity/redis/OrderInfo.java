package kr.bb.order.entity.redis;

import bloomingblooms.domain.order.OrderInfoByStore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.persistence.Id;
import kr.bb.order.dto.request.orderForDelivery.OrderForDeliveryRequest;
import kr.bb.order.entity.OrderType;
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

  @JsonProperty(value = "subscriptionPay")
  private boolean isSubscriptionPay;

  private Long deliveryAddressId;
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
  private String orderType;

  public static OrderInfo convertToRedisDto(
      String tempOrderId,
      Long userId,
      String itemName,
      int quantity,
      boolean isSubscriptionPay,
      String tid,
      OrderForDeliveryRequest requestDto,
      OrderType orderType) {

    return OrderInfo.builder()
        .tempOrderId(tempOrderId)
        .userId(userId)
        .itemName(itemName)
        .sumOfAllQuantity((long) quantity)
        .orderInfoByStores(requestDto.getOrderInfoByStores())
        .sumOfActualAmount(requestDto.getSumOfActualAmount())
        .isSubscriptionPay(isSubscriptionPay)
        .deliveryAddressId(requestDto.getDeliveryAddressId())
        .ordererName(requestDto.getOrdererName())
        .ordererPhoneNumber(requestDto.getOrdererPhoneNumber())
        .ordererEmail(requestDto.getOrdererEmail())
        .recipientName(requestDto.getRecipientName())
        .deliveryZipcode(requestDto.getDeliveryZipcode())
        .deliveryRoadName(requestDto.getDeliveryRoadName())
        .deliveryAddressDetail(requestDto.getDeliveryAddressDetail())
        .recipientPhone(requestDto.getRecipientPhone())
        .deliveryRequest(requestDto.getDeliveryRequest())
        .tid(tid)
        .orderType(orderType.toString())
        .build();
  }

  public void setPgToken(String pgToken) {
    this.pgToken = pgToken;
  }
}
