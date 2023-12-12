package kr.bb.order.repository;

import java.util.List;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.entity.delivery.OrderGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderDeliveryRepository extends JpaRepository<OrderDelivery, String> {
    @Query("select od from OrderDelivery od where od.orderGroup in :orderGroups")
    List<OrderDelivery> findAllByOrderGroups(List<OrderGroup> orderGroups);

    List<OrderDelivery> findByOrderGroup_OrderGroupId(String orderGroupId);
}
