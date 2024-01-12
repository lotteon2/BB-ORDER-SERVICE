package kr.bb.order.repository;

import java.time.LocalDate;
import java.util.List;
import kr.bb.order.dto.WeeklySalesDto;
import kr.bb.order.entity.pickup.OrderPickup;
import kr.bb.order.util.StoreIdAndTotalAmountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderPickupRepository extends JpaRepository<OrderPickup, String> {

   @Query(
      value = "SELECT DATE(op.created_at) as date, SUM(op.order_pickup_total_amount) as totalSales "
          + "FROM order_pickup op "
          + "WHERE op.store_id = :storeId AND DATE(op.created_at) BETWEEN :startDate AND :endDate "
          + "GROUP BY DATE(op.created_at)", nativeQuery = true)
  List<WeeklySalesDto> findWeeklySales(Long storeId, String startDate, String endDate);

 @Query("SELECT NEW kr.bb.order.util.StoreIdAndTotalAmountProjection(o.storeId, o.orderPickupTotalAmount) " +
       "FROM OrderPickup o " +
       "WHERE o.createdAt >= :startDate AND o.createdAt < :endDate")
List<StoreIdAndTotalAmountProjection> findAllStoreIdAndTotalAmountForDateRange(@Param("startDate") LocalDate startDate,
                                                                              @Param("endDate") LocalDate endDate);

}
