package kr.bb.order.repository;

import bloomingblooms.domain.notification.delivery.DeliveryStatus;
import kr.bb.order.entity.delivery.OrderGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderGroupRepository extends JpaRepository<OrderGroup, String> {
  @Query(
      "SELECT DISTINCT og FROM OrderGroup og JOIN og.orderDeliveryList od WHERE og.userId = :userId AND od.orderDeliveryStatus = :status ORDER BY og.createdAt DESC")
  Page<OrderGroup> findByUserIdAndOrderDeliveryStatusSortedByCreatedAtDesc(
      Long userId, Pageable pageable, DeliveryStatus status);
}
