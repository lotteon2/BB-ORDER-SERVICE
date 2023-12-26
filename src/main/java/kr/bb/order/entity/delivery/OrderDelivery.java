package kr.bb.order.entity.delivery;

import bloomingblooms.domain.order.OrderInfoByStore;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import kr.bb.order.entity.OrderDeliveryProduct;
import kr.bb.order.entity.common.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Builder
@Table(name = "order_delivery")
@AllArgsConstructor
@NoArgsConstructor
public class OrderDelivery extends BaseEntity {
  @Id private String orderDeliveryId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_group_id")
  private OrderGroup orderGroup;

  @Builder.Default
  @OneToMany(mappedBy = "orderDelivery", cascade = CascadeType.PERSIST, orphanRemoval = true)
  private List<OrderDeliveryProduct> orderDeliveryProducts = new ArrayList<>();

  @Column(name = "store_id", nullable = false)
  private Long storeId;

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

  public static OrderDelivery toEntity(
      String orderDeliveryId,
      Long deliveryId,
      OrderGroup orderGroup,
      OrderInfoByStore orderInfoByStore) {
    return OrderDelivery.builder()
        .orderDeliveryId(orderDeliveryId)
        .orderGroup(orderGroup)
        .storeId(orderInfoByStore.getStoreId())
        .deliveryId(deliveryId)
        .orderDeliveryTotalAmount(orderInfoByStore.getTotalAmount())
        .orderDeliveryCouponAmount(orderInfoByStore.getCouponAmount())
        .build();
  }

  public void setOrderGroup(OrderGroup orderGroup) {
    this.orderGroup = orderGroup;
    orderGroup.getOrderDeliveryList().add(this);
  }

  public void setOrderDeliveryProduct(List<OrderDeliveryProduct> orderDeliveryProducts) {
    this.orderDeliveryProducts = orderDeliveryProducts;
  }

  public void updateStatus(String newStatus){
    this.orderDeliveryStatus = OrderDeliveryStatus.valueOf(newStatus);
  }
}
