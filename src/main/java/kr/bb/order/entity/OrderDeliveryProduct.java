package kr.bb.order.entity;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import kr.bb.order.entity.common.BaseEntity;
import kr.bb.order.entity.delivery.OrderDelivery;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "order_delivery_product")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDeliveryProduct extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long orderProductId;
  @NotNull private String productId;
  @NotNull private Long orderProductPrice;
  @NotNull private Long orderProductQuantity;
  @Builder.Default @NotNull private ReviewStatus reviewIsWritten = ReviewStatus.DISABLED;
  @Builder.Default @NotNull private CardStatus cardIsWritten = CardStatus.ABLE;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_delivery_id")
  private OrderDelivery orderDelivery;

  public void setOrderDelivery(OrderDelivery orderDelivery) {
    this.orderDelivery = orderDelivery;
  }
}
