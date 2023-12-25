package kr.bb.order.repository.settlement;

import java.util.List;
import kr.bb.order.entity.settlement.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementJpaRepository extends JpaRepository<Settlement, Long> {

  Page<Settlement> findAll(Specification<Settlement> specification, Pageable pageable);

   @Query("SELECT s FROM Settlement s ORDER BY s.settlementAmount DESC")
    List<Settlement> findTop10ByOrderBySettlementAmountDesc();
}
