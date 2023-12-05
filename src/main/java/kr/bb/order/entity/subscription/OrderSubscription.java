package kr.bb.order.entity.subscription;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
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
@Table(name="order_subscription")
@AllArgsConstructor
@NoArgsConstructor
public class OrderSubscription extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderSubscriptionId;
    @NotNull
    private Long userId;
    @NotNull
    private Long subscriptionProductId;
    @NotNull
    private Long deliveryId;
    @NotNull
    private String productName;
    @NotNull
    private Long productPrice;
    @NotNull
    private Long deliveryDay;
}
