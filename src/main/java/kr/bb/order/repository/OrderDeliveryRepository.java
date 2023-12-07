package kr.bb.order.repository;

import java.util.List;
import java.util.Optional;
import kr.bb.order.entity.delivery.OrderDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface OrderDeliveryRepository extends JpaRepository<OrderDelivery, Long> {
    List<OrderDelivery> findByOrderGroupId(String orderGroupId);
}
