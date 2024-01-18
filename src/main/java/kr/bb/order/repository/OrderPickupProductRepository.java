package kr.bb.order.repository;

import kr.bb.order.entity.OrderPickupProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderPickupProductRepository extends JpaRepository<OrderPickupProduct, Long> {}
