package kr.bb.order.dto.request.orderForSubscription;

import bloomingblooms.domain.order.ProductCreate;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderForSubscriptionDto {
  private Long storeId;
  private String storeName;
  private LocalDate paymentDay;
  private LocalDate deliveryDay;
  private ProductCreate products;
  private Long totalAmount;
  private Long deliveryCost;
  private Long couponId;
  private Long couponAmount;
  private Long actualAmount;
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
}
