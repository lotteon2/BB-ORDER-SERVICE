package kr.bb.order.service.settlement.repository;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import kr.bb.order.entity.settlement.Settlement;
import kr.bb.order.repository.settlement.SettlementJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class GetTop10SettlementServiceRepositoryTest {

  @Autowired
  private SettlementJpaRepository repository;

  private void createSettlement(Long storeId, LocalDateTime settlementDate,
      Long settlementAmount) {
    Settlement settlement = Settlement.builder().storeId(storeId).settlementAmount(settlementAmount)
        .settlementDate(settlementDate)
        .build();
    repository.save(settlement);

  }

  @DisplayName("데이터 DESC 내림차순 테스트")
  @Test
  void GetTop10Data_WhenDataIsExisted_GetDataByDescOrder() {
    createSettlement(1L, LocalDateTime.of(2023, 11, 15, 12, 30), 1500L);
    createSettlement(2L, LocalDateTime.of(2023, 11, 15, 12, 30), 1600L);
    createSettlement(3L, LocalDateTime.of(2023, 11, 15, 12, 30), 1700L);
    createSettlement(4L, LocalDateTime.of(2023, 11, 15, 12, 30), 1800L);
    createSettlement(5L, LocalDateTime.of(2023, 11, 15, 12, 30), 1900L);
    createSettlement(6L, LocalDateTime.of(2023, 11, 15, 12, 30), 2000L);
    createSettlement(7L, LocalDateTime.of(2023, 11, 15, 12, 30), 2100L);
    createSettlement(8L, LocalDateTime.of(2023, 11, 15, 12, 30), 2200L);
    createSettlement(9L, LocalDateTime.of(2023, 11, 15, 12, 30), 2300L);
    createSettlement(10L, LocalDateTime.of(2023, 11, 15, 12, 30), 2400L);
    createSettlement(11L, LocalDateTime.of(2023, 11, 15, 12, 30), 2660L);
    createSettlement(12L, LocalDateTime.of(2023, 11, 15, 12, 30), 2800L);

    List<Settlement> top10Settlements = repository.findTop10ByOrderBySettlementAmountDesc(
        PageRequest.of(0, 10));
    assertNotNull(top10Settlements);
    assertEquals(10, top10Settlements.size());

    for (int i = 0; i < top10Settlements.size() - 1; i++) {
      assertTrue(top10Settlements.get(i).getSettlementAmount() >= top10Settlements.get(i + 1)
          .getSettlementAmount());
    }

  }

}
