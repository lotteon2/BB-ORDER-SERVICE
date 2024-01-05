package kr.bb.order.entity.redis;

import bloomingblooms.domain.notification.order.OrderType;
import bloomingblooms.domain.order.ProductCreate;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.persistence.Id;
import kr.bb.order.dto.request.orderForSubscription.OrderForSubscriptionDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@RedisHash(value = "subscriptionOrderInfo", timeToLive = 300)
public class SubscriptionOrderInfo {
  @Id private String tempOrderId;
  private Long userId;
  private String itemName;
  private Long quantity;
  private Long storeId;
  private String storeName;
  private ProductCreate product;
  private Long totalAmount; // 총 상품금액
  private Long deliveryCost; // 총 배송비
  private Long couponId;
  private Long couponAmount; // 총 할인금액
  private Long actualAmount; //  실 결제금액

  @JsonProperty(value = "subscriptionPay")
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
  private Long deliveryAddressId;
  private String tid;
  private String pgToken; // 결제승인 진입시 발급
  private String orderType;

  public static SubscriptionOrderInfo convertToRedisDto(
          String tempOrderId,
          Long userId,
          String itemName,
          Long quantity,
          boolean isSubscriptionPay,
          String tid,
          OrderForSubscriptionDto requestDto,
          OrderType orderType){

    return SubscriptionOrderInfo.builder()
            .tempOrderId(tempOrderId)
            .userId(userId)
            .itemName(itemName)
            .quantity(quantity)
            .storeId(requestDto.getStoreId())
            .storeName(requestDto.getStoreName())
            .product(requestDto.getProducts())
            .totalAmount(requestDto.getTotalAmount())
            .deliveryCost(requestDto.getDeliveryCost())
            .couponId(requestDto.getCouponId())
            .couponAmount(requestDto.getCouponAmount())
            .actualAmount(requestDto.getActualAmount())
            .isSubscriptionPay(isSubscriptionPay)
            .ordererName(requestDto.getOrdererName())
            .ordererPhoneNumber(requestDto.getOrdererPhoneNumber())
            .ordererEmail(requestDto.getOrdererEmail())
            .recipientName(requestDto.getRecipientName())
            .deliveryZipcode(requestDto.getDeliveryZipcode())
            .deliveryRoadName(requestDto.getDeliveryRoadName())
            .deliveryAddressDetail(requestDto.getDeliveryAddressDetail())
            .recipientPhone(requestDto.getRecipientPhone())
            .deliveryRequest(requestDto.getDeliveryRequest())
            .deliveryAddressId(requestDto.getDeliveryAddressId())
            .tid(tid)
            .orderType(orderType.toString())
            .build();
  }

  public void setPgToken(String pgToken){ this.pgToken = pgToken;}
}
