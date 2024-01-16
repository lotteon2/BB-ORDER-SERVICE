package kr.bb.order.entity.subscription;

import bloomingblooms.domain.review.ReviewStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import kr.bb.order.entity.common.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Builder
@Table(name = "order_subscription")
@AllArgsConstructor
@NoArgsConstructor
public class OrderSubscription extends BaseEntity {
  @Id private String orderSubscriptionId;
  @NotNull private Long userId;
  @NotNull private String subscriptionProductId;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private SubscriptionStatus subscriptionStatus = SubscriptionStatus.COMPLETED;

  @NotNull private Long deliveryId;
  @NotNull private String productName;
  @NotNull private Long productPrice;
  @NotNull private LocalDate deliveryDay;
  @NotNull private Long storeId;
  @NotNull private String phoneNumber;
  @NotNull private LocalDateTime paymentDate;
  private LocalDateTime endDate;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private ReviewStatus reviewStatus = ReviewStatus.DISABLED;

  public void updateStatus(SubscriptionStatus status) {
    this.subscriptionStatus = status;
  }

  public void updateReviewStatus(ReviewStatus reviewStatus) {
    this.reviewStatus = reviewStatus;
  }
}
