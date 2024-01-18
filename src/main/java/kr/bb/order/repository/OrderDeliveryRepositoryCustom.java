package kr.bb.order.repository;

import bloomingblooms.domain.notification.delivery.DeliveryStatus;
import kr.bb.order.entity.delivery.OrderDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface OrderDeliveryRepositoryCustom {
  Page<OrderDelivery> findByStoreIdSortedByCreatedAtDesc(
      Long storeId, Pageable pageable, @Param("status") DeliveryStatus status);
}
