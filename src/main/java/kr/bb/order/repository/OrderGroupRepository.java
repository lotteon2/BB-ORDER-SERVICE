package kr.bb.order.repository;

import bloomingblooms.domain.notification.delivery.DeliveryStatus;
import kr.bb.order.entity.delivery.OrderGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderGroupRepository extends JpaRepository<OrderGroup, String>, OrderGroupRepositoryCustom {
}
