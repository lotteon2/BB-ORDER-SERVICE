package kr.bb.order.repository.settlement;

import java.time.LocalDateTime;
import javax.persistence.criteria.Predicate;
import kr.bb.order.entity.settlement.Settlement;
import org.springframework.data.jpa.domain.Specification;


public class SettlementSpecification {

    private SettlementSpecification() {

    }

public static Specification<Settlement> filterSettlements(Long storeId, Integer year, Integer month) {
    return (root, query, criteriaBuilder) -> {
        Predicate predicate = criteriaBuilder.conjunction();

        if (storeId != null) {
            predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.equal(root.get("storeId"), storeId));
        }

        if (year != null) {
            if (month != null) {
                LocalDateTime startDateTime = LocalDateTime.of(year, month, 1, 0, 0);
                LocalDateTime endDateTime = startDateTime.plusMonths(1).minusSeconds(1);

                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.between(root.get("settlementDate"), startDateTime, endDateTime)
                );
            } else {
                LocalDateTime startDateTime = LocalDateTime.of(year, 1, 1, 0, 0);
                LocalDateTime endDateTime = startDateTime.plusYears(1).minusSeconds(1);

                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.between(root.get("settlementDate"), startDateTime, endDateTime)
                );
            }
        }

        return predicate;
    };
}
}