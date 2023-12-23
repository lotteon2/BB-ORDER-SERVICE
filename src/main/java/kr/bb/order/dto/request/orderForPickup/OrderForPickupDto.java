package kr.bb.order.dto.request.orderForPickup;

import bloomingblooms.domain.order.ProductCreate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderForPickupDto {
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
  private String ordererName;
  private String ordererPhoneNumber;
  private String ordererEmail;
}
