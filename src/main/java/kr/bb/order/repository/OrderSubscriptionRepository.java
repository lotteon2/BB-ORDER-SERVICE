package kr.bb.order.repository;

import java.time.LocalDateTime;
import java.util.List;
import kr.bb.order.entity.subscription.OrderSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderSubscriptionRepository extends JpaRepository<OrderSubscription, String> {
  @Query(
      "SELECT os.createdAt AS date, SUM(os.productPrice) as totalAmount "
          + "FROM OrderSubscription os "
          + "WHERE os.storeId = :storeId AND DATE(os.createdAt) BETWEEN :startDate AND :endDate "
          + "GROUP BY DATE(os.createdAt)")
  List<Object[]> findWeeklySales(Long storeId, LocalDateTime startDate, LocalDateTime endDate);
}
