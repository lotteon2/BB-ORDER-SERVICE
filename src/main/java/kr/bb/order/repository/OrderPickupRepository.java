package kr.bb.order.repository;

import java.time.LocalDateTime;
import java.util.List;
import kr.bb.order.entity.pickup.OrderPickup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderPickupRepository extends JpaRepository<OrderPickup, String> {
  @Query(
      "SELECT op.createdAt AS date, SUM(op.orderPickupTotalAmount) as totalAmount "
          + "FROM OrderPickup op "
          + "WHERE op.storeId = :storeId AND DATE(op.createdAt) BETWEEN :startDate AND :endDate "
          + "GROUP BY DATE(op.createdAt)")
  List<Object[]> findWeeklySales(Long storeId, LocalDateTime startDate, LocalDateTime endDate);
}
