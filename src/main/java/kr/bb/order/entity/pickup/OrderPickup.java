package kr.bb.order.entity.pickup;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import kr.bb.order.entity.OrderPickupProduct;
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
    private String orderPickupId;

    @OneToOne(mappedBy = "orderPickup")
    private OrderPickupProduct orderPickupProduct;

    @Column(name="user_id", nullable = false)
    private Long userId;

    @Column(name="store_id", nullable = false)
    private Long storeId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name="order_pickup_status", nullable = false)
    private OrderPickupStatus orderPickupStatus = OrderPickupStatus.PENDING;

    @Column(name="order_pickup_total_amount", nullable = false)
    private Long orderPickupTotalAmount;

    @Column(name="order_pickup_coupon_amount", nullable= false)
    private Long orderPickupCouponAmount;

    @Builder.Default
    @Column(name="order_pickup_is_complete", nullable = false)
    private Boolean orderPickupIsComplete = false;

    @Column(name="order_pickup_datetime", nullable = false)
    private LocalDateTime orderPickupDatetime;

    @Column(name="order_pickup_phone_number", nullable = false)
    private String orderPickupPhoneNumber;

    public void setOrderPickupProduct(OrderPickupProduct orderPickupProduct){
        this.orderPickupProduct = orderPickupProduct;
    }

    public void completeOrderPickup(OrderPickupStatus orderPickupStatus ) {
        if(orderPickupStatus.equals(OrderPickupStatus.COMPLETED)){
            this.orderPickupIsComplete = true;
            this.orderPickupStatus = OrderPickupStatus.COMPLETED;
        }
        else{
            this.orderPickupStatus = OrderPickupStatus.CANCELED;
        }
        this.orderPickupProduct.updateCardAndReviewStatus(orderPickupStatus);
    }

    public void updateStatus(OrderPickupStatus orderPickupStatus ){
        this.orderPickupStatus = orderPickupStatus;
    }
}
