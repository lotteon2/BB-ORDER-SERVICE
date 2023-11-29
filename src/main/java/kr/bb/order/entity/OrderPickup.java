package kr.bb.order.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import kr.bb.order.entity.common.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name ="order_pickup")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderPickup extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderPickupId;

    @Column(name="user_id", nullable = false)
    private Long userId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name="order_pickup_status", nullable = false)
    private OrderPickupStatus orderPickupStatus = OrderPickupStatus.PENDING;

    @Column(name="order_pickup_total_amount", nullable = false)
    private Long orderPickupTotalAmount;

    @Column(name="order_pickup_coupon_amount", nullable= false)
    private Long orderPickupCouponAmount;

    @Column(name="order_pickup_is_complete", nullable = false)
    private Boolean orderPickupIsComplete;

    @Column(name="order_pickup_datetime", nullable = false)
    private LocalDateTime orderPickupDatetime;
}
