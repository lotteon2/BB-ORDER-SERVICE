package kr.bb.order.entity;

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
@Table(name="order_product")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderProduct extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderProductId;
    @NotNull
    private String orderType;
    @NotNull
    private Long productId;
    @NotNull
    private Long orderProductPrice;
    @NotNull
    private Long orderProductQuantity;
    @NotNull
    private Boolean reviewIsWritten;
    @NotNull
    private Boolean cardIsWritten;
}
