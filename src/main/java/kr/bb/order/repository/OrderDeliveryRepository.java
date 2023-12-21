package kr.bb.order.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.entity.delivery.OrderDeliveryStatus;
import kr.bb.order.entity.delivery.OrderGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderDeliveryRepository extends JpaRepository<OrderDelivery, String> {
    @Query("select od from OrderDelivery od where od.orderGroup in :orderGroups")
    List<OrderDelivery> findAllByOrderGroups(List<OrderGroup> orderGroups);

    @Query("select od from OrderDelivery od where od.orderGroup.orderGroupId = :orderGroupId")
    List<OrderDelivery> findByOrderGroupId(String orderGroupId);

    @Query("SELECT od FROM OrderDelivery od WHERE od.storeId = :storeId AND od.orderDeliveryStatus = :status ORDER BY od.createdAt DESC")
    Page<OrderDelivery> findByStoreIdSortedByCreatedAtDesc(Long storeId, Pageable pageable, OrderDeliveryStatus status);

    @Query("SELECT od.createdAt AS date, SUM(od.orderDeliveryTotalAmount) as totalAmount " + "FROM OrderDelivery od "
        + "WHERE od.storeId = :storeId AND od.createdAt >= :startDate AND od.createdAt <= :endDate "
            + "GROUP BY od.createdAt")
    List<Object[]> findWeeklySales(Long storeId, LocalDateTime startDate, LocalDateTime endDate);

}
