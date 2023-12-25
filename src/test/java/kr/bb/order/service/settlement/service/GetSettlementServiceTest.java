package kr.bb.order.service.settlement.service;

import java.time.LocalDateTime;
import kr.bb.order.entity.settlement.Settlement;
import kr.bb.order.repository.settlement.SettlementJpaRepository;
import kr.bb.order.service.settlement.GetSettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GetSettlementServiceTest {

  @Autowired
  private GetSettlementService getSettlementService;
  @Autowired
  private SettlementJpaRepository repository;

  private void createSettlement(Long storeId, String storeName, LocalDateTime settlementDate,
      Long settlementAmount) {
    Settlement settlement = Settlement.builder().storeId(storeId).storeName(storeName)
        .settlementAmount(settlementAmount)
        .settlementDate(settlementDate)
        .build();
    repository.save(settlement);
  }


  @DisplayName("Set pre-data which is arranged by ACES")
  @BeforeEach
  void init() {

    createSettlement(1L, "StoreA", LocalDateTime.of(2023, 11, 15, 12, 30), 1500L);
    createSettlement(2L, "StoreB", LocalDateTime.of(2023, 10, 10, 9, 0), 2000L);
    createSettlement(3L, "StoreC", LocalDateTime.of(2023, 10, 10, 9, 0), 2500L);
    createSettlement(4L, "StoreD", LocalDateTime.of(2023, 11, 15, 12, 30), 3000L);
    createSettlement(5L, "StoreE", LocalDateTime.of(2023, 10, 10, 9, 0), 4000L);
    createSettlement(6L, "StoreF", LocalDateTime.of(2023, 10, 10, 9, 0), 5000L);
    createSettlement(7L, "StoreG", LocalDateTime.of(2023, 11, 15, 12, 30), 6000L);
    createSettlement(8L, "StoreH", LocalDateTime.of(2023, 10, 10, 9, 0), 7000L);
    createSettlement(9L, "StoreI", LocalDateTime.of(2023, 10, 10, 9, 0), 8000L);
    createSettlement(10L, "StoreJ", LocalDateTime.of(2023, 11, 15, 12, 30), 9000L);
    createSettlement(11L, "StoreK", LocalDateTime.of(2023, 10, 10, 9, 0), 10000L);
    createSettlement(12L, "StoreL", LocalDateTime.of(2023, 10, 10, 9, 0), 11000L);

  }

  @DisplayName("데이터가 존재할 때 SettlementDto List NotNull 데이터 list 얻기")
  @Test
  void GetListOfSettlementDto_WhenThereIsPreData_GetEachOfElementsNotNullSettlementDto() {
  }


}
