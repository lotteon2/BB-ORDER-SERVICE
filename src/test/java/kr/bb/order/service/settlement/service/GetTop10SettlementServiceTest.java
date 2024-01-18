package kr.bb.order.service.settlement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import kr.bb.order.dto.response.settlement.BestSettlementDto;
import kr.bb.order.dto.response.settlement.LastMonthTop10SalesResponse;
import kr.bb.order.entity.settlement.Settlement;
import kr.bb.order.repository.settlement.SettlementJpaRepository;
import kr.bb.order.service.settlement.GetTop10SettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;

@SpringBootTest
class GetTop10SettlementServiceTest {

  @Autowired private GetTop10SettlementService getTop10SettlementService;
  @Autowired private SettlementJpaRepository repository;
  @MockBean private SimpleMessageListenerContainer simpleMessageListenerContainer;

  private void createSettlement(
      Long storeId,
      String storeName,
      LocalDateTime settlementDate,
      Long settlementAmount,
      String sido,
      String gugun) {
    Settlement settlement =
        Settlement.builder()
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
    createSettlement(
        1L, "StoreA", LocalDateTime.of(2023, 11, 15, 12, 30), 1500L, "SidoA", "GugunA");
    createSettlement(2L, "StoreB", LocalDateTime.of(2023, 10, 10, 9, 0), 2000L, "SidoB", "GugunB");
    createSettlement(3L, "StoreC", LocalDateTime.of(2023, 10, 10, 9, 0), 2500L, "SidoC", "GugunC");
    createSettlement(
        4L, "StoreD", LocalDateTime.of(2023, 11, 15, 12, 30), 3000L, "SidoD", "GugunD");
    createSettlement(5L, "StoreE", LocalDateTime.of(2023, 10, 10, 9, 0), 4000L, "SidoE", "GugunE");
    createSettlement(6L, "StoreF", LocalDateTime.of(2023, 10, 10, 9, 0), 5000L, "SidoF", "GugunF");
    createSettlement(
        7L, "StoreG", LocalDateTime.of(2023, 11, 15, 12, 30), 6000L, "SidoG", "GugunG");
    createSettlement(8L, "StoreH", LocalDateTime.of(2023, 10, 10, 9, 0), 7000L, "SidoH", "GugunH");
    createSettlement(9L, "StoreI", LocalDateTime.of(2023, 10, 10, 9, 0), 8000L, "SidoI", "GugunI");
    createSettlement(
        10L, "StoreJ", LocalDateTime.of(2023, 11, 15, 12, 30), 9000L, "SidoJ", "GugunJ");
    createSettlement(
        11L, "StoreK", LocalDateTime.of(2023, 10, 10, 9, 0), 10000L, "SidoK", "GugunK");
    createSettlement(
        12L, "StoreL", LocalDateTime.of(2023, 10, 10, 9, 0), 11000L, "SidoL", "GugunL");
  }

  @DisplayName(
      "데이터가 존재할 때 Top10SalesResponse가 가지는 BestSettlementDto의 element가 모두 Not null이고 DESC 상태")
  @Test
  void GetLastMonthTop10SalesResponse_WhenThereIsData_AllListInResponseNotNull() {

    LastMonthTop10SalesResponse response = getTop10SettlementService.getTop10();

    List<BestSettlementDto> bestSettlementDtoList = response.getBestSettlementDtoList();

    assertNotNull(bestSettlementDtoList);
    assertFalse(bestSettlementDtoList.isEmpty());

    for (BestSettlementDto dto : bestSettlementDtoList) {
      assertNotNull(dto);
      assertNotNull(dto.getName());
      assertNotNull(dto.getData());
    }

    for (int i = 0; i < bestSettlementDtoList.size() - 1; i++) {
      long currentAmount = bestSettlementDtoList.get(i).getData().get(0);
      long nextAmount = bestSettlementDtoList.get(i + 1).getData().get(0);
      assertTrue(currentAmount >= nextAmount);
    }
    assertEquals(10, bestSettlementDtoList.size());
  }
}
