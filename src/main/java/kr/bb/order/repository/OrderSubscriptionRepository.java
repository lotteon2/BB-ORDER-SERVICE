package kr.bb.order.repository;

import kr.bb.order.entity.subscription.OrderSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderSubscriptionRepository extends JpaRepository<OrderSubscription, String> {}
