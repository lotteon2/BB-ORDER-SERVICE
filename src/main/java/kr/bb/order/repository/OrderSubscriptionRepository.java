package kr.bb.order.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.bb.order.dto.WeeklySalesDto;
import kr.bb.order.entity.subscription.OrderSubscription;
import kr.bb.order.entity.subscription.SubscriptionStatus;
import kr.bb.order.util.StoreIdAndTotalAmountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderSubscriptionRepository extends JpaRepository<OrderSubscription, String> {

  @Query(
      value = "SELECT DATE(os.created_at) AS DATE, SUM(os.product_price) AS totalSales "
          + "FROM order_subscription os "
          + "WHERE os.store_id = :storeId AND DATE(os.created_at) BETWEEN :startDate AND :endDate "
          + "GROUP BY DATE(os.created_at)", nativeQuery = true)
  List<WeeklySalesDto> findWeeklySales(Long storeId, String startDate, String endDate);

  @Query(
      "SELECT NEW kr.bb.order.util.StoreIdAndTotalAmountProjection(o.storeId, o.productPrice)"+
          "FROM OrderSubscription o " +
          "WHERE o.createdAt >= :startDate AND o.createdAt < :endDate AND NOT o.subscriptionStatus = :subscriptionStatus")
  List<StoreIdAndTotalAmountProjection> findAllStoreIdAndTotalAmountForDateRangeAndNotOrderPickupStatus(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      @Param("subscriptionStatus") SubscriptionStatus subscriptionStatus);
}
