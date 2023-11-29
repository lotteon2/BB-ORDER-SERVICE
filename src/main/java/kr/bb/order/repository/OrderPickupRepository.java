package kr.bb.order.repository;

import kr.bb.order.entity.OrderPickup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderPickupRepository extends JpaRepository<OrderPickup, Long> {}
