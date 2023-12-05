package kr.bb.order.entity.delivery;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import kr.bb.order.dto.request.orderForDelivery.OrderInfoByStore;
import kr.bb.order.entity.common.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Builder
@Table(name = "order_delivery")
@AllArgsConstructor
@NoArgsConstructor
public class OrderDelivery extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long orderDeliveryId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "delivery_id", nullable = false)
  private Long deliveryId;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "order_delivery_status", nullable = false)
  private OrderDeliveryStatus orderDeliveryStatus = OrderDeliveryStatus.PENDING;

  @Column(name = "order_delivery_total_amount", nullable = false)
  private Long orderDeliveryTotalAmount;

  @Column(name = "order_delivery_coupon_amount", nullable = false)
  private Long orderDeliveryCouponAmount;

  @Column(name = "order_group_id", nullable = false)
  private Long orderGroupId;

  public static OrderDelivery toDto(
      Long deliveryId, Long userId, Long orderGroupId, OrderInfoByStore orderInfoByStore) {
    return OrderDelivery.builder()
        .userId(userId)
        .deliveryId(deliveryId)
        .orderDeliveryStatus(OrderDeliveryStatus.PENDING)
        .orderDeliveryTotalAmount(orderInfoByStore.getTotalAmount())
        .orderDeliveryCouponAmount(orderInfoByStore.getCouponAmount())
        .orderGroupId(orderGroupId)
        .build();
  }
}
