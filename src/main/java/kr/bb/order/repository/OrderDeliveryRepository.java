package kr.bb.order.repository;

import bloomingblooms.domain.notification.delivery.DeliveryStatus;
import java.time.LocalDate;
import java.util.List;
import kr.bb.order.dto.WeeklySalesDto;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.entity.delivery.OrderGroup;
import kr.bb.order.util.StoreIdAndTotalAmountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderDeliveryRepository extends JpaRepository<OrderDelivery, String> {
  @Query("select od from OrderDelivery od where od.orderGroup in :orderGroups")
  List<OrderDelivery> findAllByOrderGroups(List<OrderGroup> orderGroups);

  @Query("select od from OrderDelivery od where od.orderGroup.orderGroupId = :orderGroupId")
  List<OrderDelivery> findByOrderGroupId(String orderGroupId);

  @Query(
      "SELECT od FROM OrderDelivery od WHERE od.storeId = :storeId AND od.orderDeliveryStatus = :status ORDER BY od.createdAt DESC")
  Page<OrderDelivery> findByStoreIdSortedByCreatedAtDesc(
      Long storeId, Pageable pageable, @Param("status") DeliveryStatus status);

  @Query(
      value = " SELECT DATE(od.created_at) AS date, SUM(od.order_delivery_total_amount) AS totalSales "
          + "FROM order_delivery od "
          + "WHERE od.store_id  = :storeId AND DATE(od.created_at) BETWEEN :startDate AND :endDate "
          + "GROUP BY DATE(od.created_at)", nativeQuery = true)
  List<WeeklySalesDto> findWeeklySales(Long storeId, String startDate, String endDate);

  @Query("SELECT NEW kr.bb.order.util.StoreIdAndTotalAmountProjection(o.storeId, o.orderDeliveryTotalAmount) " +
       "FROM OrderDelivery o " +
       "WHERE o.createdAt >= :startDate AND o.createdAt < :endDate")
List<StoreIdAndTotalAmountProjection> findAllStoreIdAndTotalAmountForDateRange(@Param("startDate") LocalDate startDate,
                                                                              @Param("endDate") LocalDate endDate);

}
