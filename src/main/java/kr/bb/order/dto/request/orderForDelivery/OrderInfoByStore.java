package kr.bb.order.dto.request.orderForDelivery;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderInfoByStore {
  private Long storeId;
  private String storeName;
  private List<ProductCreate> products;
  private Long totalAmount;
  private Long deliveryCost;
  private Long couponId;
  private Long couponAmount;
  private Long actualAmount;
  private Boolean isSubscriptionPay;
}
