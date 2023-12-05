package kr.bb.order.repository;

import kr.bb.order.entity.delivery.OrderDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface OrderDeliveryRepository extends JpaRepository<OrderDelivery, Long> {}
