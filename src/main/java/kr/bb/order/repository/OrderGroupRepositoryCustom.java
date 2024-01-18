package kr.bb.order.repository;

import bloomingblooms.domain.notification.delivery.DeliveryStatus;
import kr.bb.order.entity.delivery.OrderGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderGroupRepositoryCustom {

  Page<OrderGroup> findByUserIdAndOrderDeliveryStatusSortedByCreatedAtDesc(
      Long userId, Pageable pageable, DeliveryStatus status);
}
