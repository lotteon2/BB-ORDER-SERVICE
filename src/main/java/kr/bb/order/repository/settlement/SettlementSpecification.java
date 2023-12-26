package kr.bb.order.repository.settlement;

import java.time.LocalDateTime;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import kr.bb.order.entity.settlement.Settlement;
import org.springframework.data.jpa.domain.Specification;


public class SettlementSpecification {

    private SettlementSpecification() {
    }

    public static Specification<Settlement> filterSettlements(String sido, String gugun,
        Long storeId, Integer year,
        Integer month) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();

            if (storeId != null) {
                predicate = criteriaByStoreId(criteriaBuilder, root, predicate, storeId);
            }

            if (sido != null) {
                predicate = criteriaBySido(criteriaBuilder, root, predicate, sido);
            }

            if (gugun != null) {
                predicate = criteriaByGugun(criteriaBuilder, root, predicate, gugun);
            }

            if (year != null) {
                if (month != null) {
                    predicate = criteriaByYearAndMonth(criteriaBuilder, root, predicate, year,
                        month);
                } else {
                    predicate = criteriaByOnlyYear(criteriaBuilder, root, predicate, year);
                }
            }

            if (year == null && month != null) {
                predicate = criteriaByOnlyMonth(criteriaBuilder, root, predicate, month);
            }


            return predicate;
        };
    }

    private static Predicate criteriaByStoreId(CriteriaBuilder criteriaBuilder,
        Root<Settlement> root, Predicate predicate, Long storeId) {
        return criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("storeId"), storeId));
    }

    private static Predicate criteriaByYearAndMonth(CriteriaBuilder criteriaBuilder,
        Root<Settlement> root, Predicate predicate, Integer year, Integer month) {
        LocalDateTime startDateTime = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endDateTime = startDateTime.plusMonths(1).minusSeconds(1);

        return criteriaBuilder.and(predicate,
            criteriaBuilder.between(root.get("settlementDate"), startDateTime, endDateTime));
    }

    private static Predicate criteriaByOnlyYear(CriteriaBuilder criteriaBuilder,
        Root<Settlement> root, Predicate predicate, Integer year) {
        LocalDateTime startDateTime = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime endDateTime = startDateTime.plusYears(1).minusSeconds(1);

        return criteriaBuilder.and(predicate,
            criteriaBuilder.between(root.get("settlementDate"), startDateTime, endDateTime));
    }

    private static Predicate criteriaByOnlyMonth(CriteriaBuilder criteriaBuilder,
        Root<Settlement> root, Predicate predicate, Integer month) {

        return criteriaBuilder.and(
            predicate,
            criteriaBuilder.equal(
                criteriaBuilder.function("month", Integer.class, root.get("settlementDate")), month)
        );
    }

    private static Predicate criteriaBySido(CriteriaBuilder criteriaBuilder,
        Root<Settlement> root, Predicate predicate, String sido) {
        return criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("sido"), sido));
    }

    private static Predicate criteriaByGugun(CriteriaBuilder criteriaBuilder,
        Root<Settlement> root, Predicate predicate, String gugun) {
        return criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("gugun"), gugun));
    }
}