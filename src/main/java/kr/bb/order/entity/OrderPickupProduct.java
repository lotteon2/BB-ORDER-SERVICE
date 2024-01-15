package kr.bb.order.entity;

import bloomingblooms.domain.card.CardStatus;
import bloomingblooms.domain.review.ReviewStatus;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import kr.bb.order.entity.common.BaseEntity;
import kr.bb.order.entity.pickup.OrderPickup;
import kr.bb.order.entity.pickup.OrderPickupStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "order_pickup_product")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderPickupProduct extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long orderPickupProductId;

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST, orphanRemoval = true)
  @JoinColumn(name = "order_pickup_id")
  private OrderPickup orderPickup;

  @NotNull private String productId;
  @NotNull private Long orderProductPrice;
  @NotNull private Long orderProductQuantity;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @NotNull
  private ReviewStatus reviewIsWritten = ReviewStatus.DISABLED;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @NotNull
  private CardStatus cardIsWritten = CardStatus.ABLE;

  // 편의 메서드 적용
  public void setOrderPickup(OrderPickup orderPickup) {
    this.orderPickup = orderPickup;
    orderPickup.setOrderPickupProduct(this);
  }

  public void updateCardAndReviewStatus(OrderPickupStatus orderPickupStatus ){
    if(orderPickupStatus.equals(OrderPickupStatus.CANCELED)){
      this.reviewIsWritten = ReviewStatus.DISABLED;
      this.cardIsWritten = CardStatus.DISABLED;
    }
    else{
      this.reviewIsWritten = ReviewStatus.ABLE;
      if(!this.cardIsWritten.equals(CardStatus.DONE)){
        this.cardIsWritten = CardStatus.DISABLED;
      }
    }
  }
}
