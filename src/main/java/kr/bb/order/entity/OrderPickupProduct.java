package kr.bb.order.entity;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
@Table(name="order_pickup_product")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderPickupProduct extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderPickupProductId;
    @NotNull
    private String orderId;
    @NotNull
    private String productId;
    @NotNull
    private Long orderProductPrice;
    @NotNull
    private Long orderProductQuantity;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @NotNull
    private ReviewStatus reviewIsWritten = ReviewStatus.DISABLED;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @NotNull
    private CardStatus cardIsWritten = CardStatus.ABLE;
}
