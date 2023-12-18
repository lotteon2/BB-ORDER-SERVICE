package kr.bb.order.entity.redis;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalTime;
import javax.persistence.Id;
import kr.bb.order.dto.request.orderForDelivery.ProductCreate;
import kr.bb.order.dto.request.orderForPickup.OrderForPickupDto;
import kr.bb.order.entity.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@RedisHash(value = "pickupOrderInfo", timeToLive = 300)
public class PickupOrderInfo {
  @Id private String tempOrderId;
  private Long userId;
  private String itemName;
  private Long quantity;
  private Long storeId;
  private String storeName;
  private String pickupDate;
  private String pickupTime;
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
  private String tid;
  private String pgToken;
  private String orderType;

  public static PickupOrderInfo transformDataForApi(
      String tempOrderId,
      Long userId,
      String itemName,
      Long quantity,
      boolean isSubscriptionPay,
      String tid,
      OrderForPickupDto requestDto,
      OrderType orderType) {
    return PickupOrderInfo.builder()
        .tempOrderId(tempOrderId)
        .userId(userId)
        .itemName(itemName)
        .quantity(quantity)
        .storeId(requestDto.getStoreId())
        .storeName(requestDto.getStoreName())
        .pickupDate(requestDto.getPickupDate())
        .pickupTime(requestDto.getPickupTime())
        .product(requestDto.getProduct())
        .totalAmount(requestDto.getTotalAmount())
        .deliveryCost(requestDto.getDeliveryCost())
        .couponId(requestDto.getCouponId())
        .couponAmount(requestDto.getCouponAmount())
        .actualAmount(requestDto.getActualAmount())
        .isSubscriptionPay(isSubscriptionPay)
        .ordererName(requestDto.getOrdererName())
        .ordererPhoneNumber(requestDto.getOrdererPhoneNumber())
        .ordererEmail(requestDto.getOrdererEmail())
        .tid(tid)
        .orderType(orderType.toString())
        .build();
  }

  public void setPgToken(String pgToken) {
    this.pgToken = pgToken;
  }
}
