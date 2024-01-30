package kr.bb.order.repository;

import bloomingblooms.domain.notification.delivery.DeliveryStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kr.bb.order.dto.WeeklySalesDto;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.util.StoreIdAndTotalAmountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderDeliveryRepository extends JpaRepository<OrderDelivery, String>, OrderDeliveryRepositoryCustom {
  @Query("select distinct od from OrderDelivery od join fetch od.orderDeliveryProducts where od.orderGroup.orderGroupId = :orderGroupId")
  List<OrderDelivery> findByOrderGroupId(String orderGroupId);

  @Query(
      value =
          " SELECT DATE(od.created_at) AS date, SUM(od.order_delivery_total_amount) AS totalSales "
              + "FROM order_delivery od "
              + "WHERE od.store_id  = :storeId AND DATE(od.created_at) BETWEEN :startDate AND :endDate "
              + "GROUP BY DATE(od.created_at)", nativeQuery = true)
  List<WeeklySalesDto> findWeeklySales(Long storeId, String startDate, String endDate);

  @Query(
      "SELECT NEW kr.bb.order.util.StoreIdAndTotalAmountProjection(o.storeId, o.orderDeliveryTotalAmount) "
          +
          "FROM OrderDelivery o " +
          "WHERE o.createdAt >= :startDate AND o.createdAt < :endDate AND NOT o.orderDeliveryStatus = :orderDeliveryStatus")
  List<StoreIdAndTotalAmountProjection> findAllStoreIdAndTotalAmountForDateRangeAndNotOrderPickupStatus(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      @Param("orderDeliveryStatus") DeliveryStatus deliveryStatus);

  @Query("SELECT od FROM OrderDelivery od JOIN FETCH od.orderDeliveryProducts WHERE od.orderDeliveryId = :orderDeliveryId")
  Optional<OrderDelivery> findByOrderDeliveryId(String orderDeliveryId);

}
