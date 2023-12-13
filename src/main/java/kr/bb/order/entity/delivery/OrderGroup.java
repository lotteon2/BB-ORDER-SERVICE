package kr.bb.order.entity.delivery;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import kr.bb.order.entity.common.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "order_group")
public class OrderGroup extends BaseEntity {
  @Id private String orderGroupId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @OneToMany(mappedBy = "orderGroup", cascade = CascadeType.PERSIST, orphanRemoval = true)
  private List<OrderDelivery> orderDeliveryList;

  public void setOrderDeliveryList(List<OrderDelivery> orderDeliveryList){
    this.orderDeliveryList = orderDeliveryList;
  }
}
