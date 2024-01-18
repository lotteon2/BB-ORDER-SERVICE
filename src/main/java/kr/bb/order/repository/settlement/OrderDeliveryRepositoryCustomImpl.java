package kr.bb.order.repository.settlement;

import static kr.bb.order.entity.delivery.QOrderDelivery.orderDelivery;

import bloomingblooms.domain.notification.delivery.DeliveryStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.repository.OrderDeliveryRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class OrderDeliveryRepositoryCustomImpl implements OrderDeliveryRepositoryCustom {
  private final JPAQueryFactory queryFactory;

  public OrderDeliveryRepositoryCustomImpl(EntityManager em) {
    this.queryFactory = new JPAQueryFactory(em);
  }

  @Override
  public Page<OrderDelivery> findByStoreIdSortedByCreatedAtDesc(
      Long storeId, Pageable pageable, DeliveryStatus status) {

    BooleanExpression statusCondition =
        status != null ? orderDelivery.orderDeliveryStatus.eq(status) : null;

    List<OrderDelivery> content =
        queryFactory
            .selectFrom(orderDelivery)
            .where(orderDelivery.storeId.eq(storeId))
            .where(statusCondition)
            .orderBy(orderDelivery.createdAt.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    long total =
        queryFactory
            .selectFrom(orderDelivery)
            .where(orderDelivery.storeId.eq(storeId))
            .where(statusCondition)
            .fetchCount();

    Page<OrderDelivery> resultPage = new PageImpl<>(content, pageable, total);
    return resultPage;
  }
}
