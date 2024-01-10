package kr.bb.order.repository;

import java.util.List;
import kr.bb.order.dto.WeeklySalesDto;
import kr.bb.order.entity.subscription.OrderSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderSubscriptionRepository extends JpaRepository<OrderSubscription, String> {
  @Query(
      value = "SELECT DATE(os.created_at) AS DATE, SUM(os.product_price) AS totalSales "
          + "FROM order_subscription os "
          + "WHERE os.store_id = :storeId AND DATE(os.created_at) BETWEEN :startDate AND :endDate "
          + "GROUP BY DATE(os.created_at)", nativeQuery = true)
  List<WeeklySalesDto> findWeeklySales(Long storeId, String startDate, String endDate);
}
