package kr.bb.order.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import kr.bb.order.entity.common.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name="order_delivery")
@AllArgsConstructor
@NoArgsConstructor
public class OrderDelivery extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderDeliveryId;

    @Column(name="user_id", nullable = false)
    private Long userId;

    @Column(name="delivery_id", nullable = false)
    private Long deliveryId;

    @Column(name="order_delivery_status", nullable = false)
    private String orderDeliveryStatus;

    @Column(name="order_delivery_total_amount", nullable = false)
    private Long orderDeliveryTotalAmount;

    @Column(name="order_delivery_coupon_amount", nullable = false)
    private Long orderDeliveryCouponAmount;

    @Column(name="order_group_id", nullable = false)
    private Long orderGroupId;
}
