package kr.bb.order.repository;

import static kr.bb.order.entity.delivery.QOrderDelivery.orderDelivery;
import static kr.bb.order.entity.delivery.QOrderGroup.orderGroup;

import bloomingblooms.domain.notification.delivery.DeliveryStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import kr.bb.order.entity.delivery.OrderGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class OrderGroupRepositoryCustomImpl implements OrderGroupRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    public OrderGroupRepositoryCustomImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<OrderGroup> findByUserIdAndOrderDeliveryStatusSortedByCreatedAtDesc(Long userId, Pageable pageable,
            DeliveryStatus status) {

        BooleanExpression statusCondition = status != null ? orderDelivery.orderDeliveryStatus.eq(status) : null;

        List<OrderGroup> content = queryFactory
                .selectFrom(orderGroup)
                .distinct()
                .join(orderGroup.orderDeliveryList, orderDelivery)
                .where(orderGroup.userId.eq(userId))
                .where(statusCondition)
                .orderBy(orderGroup.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .selectFrom(orderGroup)
                .distinct()
                .join(orderGroup.orderDeliveryList, orderDelivery)
                .where(orderGroup.userId.eq(userId))
                .where(statusCondition)
                .fetchCount();

        Page<OrderGroup> resultPage = new PageImpl<>(content, pageable, total);
        return resultPage;
    }
}
