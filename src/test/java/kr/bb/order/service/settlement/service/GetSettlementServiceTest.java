package kr.bb.order.service.settlement.service;

import java.time.LocalDateTime;
import kr.bb.order.entity.settlement.Settlement;
import kr.bb.order.repository.settlement.SettlementJpaRepository;
import kr.bb.order.service.settlement.GetSettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GetSettlementServiceTest {

  @Autowired
  private GetSettlementService getSettlementService;
  @Autowired
  private SettlementJpaRepository repository;

 private void createSettlement(Long storeId, String storeName, LocalDateTime settlementDate,
      Long settlementAmount, String sido, String gugun) {
    Settlement settlement = Settlement.builder()
        .storeId(storeId)
        .storeName(storeName)
        .settlementAmount(settlementAmount)
        .settlementDate(settlementDate)
        .sido(sido)
        .gugun(gugun)
        .build();
    repository.save(settlement);
  }


  @DisplayName("Set pre-data which is arranged by ACES")
  @BeforeEach
  void init() {
    createSettlement(1L, "StoreA", LocalDateTime.of(2023, 11, 15, 12, 30), 1500L, "SidoA",
        "GugunA");
    createSettlement(2L, "StoreB", LocalDateTime.of(2023, 10, 10, 9, 0), 2000L, "SidoB", "GugunB");
    createSettlement(3L, "StoreC", LocalDateTime.of(2023, 10, 10, 9, 0), 2500L, "SidoC", "GugunC");
    createSettlement(4L, "StoreD", LocalDateTime.of(2023, 11, 15, 12, 30), 3000L, "SidoD",
        "GugunD");
    createSettlement(5L, "StoreE", LocalDateTime.of(2023, 10, 10, 9, 0), 4000L, "SidoE", "GugunE");
    createSettlement(6L, "StoreF", LocalDateTime.of(2023, 10, 10, 9, 0), 5000L, "SidoF", "GugunF");
    createSettlement(7L, "StoreG", LocalDateTime.of(2023, 11, 15, 12, 30), 6000L, "SidoG",
        "GugunG");
    createSettlement(8L, "StoreH", LocalDateTime.of(2023, 10, 10, 9, 0), 7000L, "SidoH", "GugunH");
    createSettlement(9L, "StoreI", LocalDateTime.of(2023, 10, 10, 9, 0), 8000L, "SidoI", "GugunI");
    createSettlement(10L, "StoreJ", LocalDateTime.of(2023, 11, 15, 12, 30), 9000L, "SidoJ",
        "GugunJ");
    createSettlement(11L, "StoreK", LocalDateTime.of(2023, 10, 10, 9, 0), 10000L, "SidoK",
        "GugunK");
    createSettlement(12L, "StoreL", LocalDateTime.of(2023, 10, 10, 9, 0), 11000L, "SidoL",
        "GugunL");
  }


}
