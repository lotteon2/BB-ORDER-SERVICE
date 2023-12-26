package kr.bb.order.service.settlement.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import kr.bb.order.entity.settlement.Settlement;
import kr.bb.order.repository.settlement.SettlementJpaRepository;
import kr.bb.order.repository.settlement.SettlementSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@DataJpaTest
class FindAllBySpecificationDataTest {

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

  private void setSidoAndGugunData() {
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

  @DisplayName("시로 정산 데이터 얻기")
  @Test
  void GetDataBySido_whenSpecificationOnlySido_getData() {
    setSidoAndGugunData();
    Specification<Settlement> specification = SettlementSpecification.filterSettlements("SidoA", null,
        null, null,
        null);

    Page<Settlement> sidoOnly = repository.findAll(specification, PageRequest.of(0, 10));

    assertEquals(1, sidoOnly.getTotalElements());

  }

  @DisplayName("시, 구군으로 정산 데이터 얻기")
  void GetDataBySidoAndGugun_whenSpecificationSidoAndGugun_getData() {
    setSidoAndGugunData();
    Specification<Settlement> specification = SettlementSpecification.filterSettlements("SidoA", "GugunA",
        null, null,
        null);

    Page<Settlement> sidoAndGugun = repository.findAll(specification, PageRequest.of(0, 10));

    assertEquals(1, sidoAndGugun.getTotalElements());

  }

  @DisplayName("시, 구군, 스토어 아이디로 정산 데이터 얻기")
  void GetDataBySidoAndGugunAndStoreId_whenSpecificationSidoAndGugunAndStoreId_getData() {
    Specification<Settlement> specification = SettlementSpecification.filterSettlements("SidoB", "GugunB",
        2L, null,
        null);

    Page<Settlement> sidoAndGugunAndStoreId= repository.findAll(specification, PageRequest.of(0, 10));

    assertEquals(1, sidoAndGugunAndStoreId.getTotalElements());
  }

  @DisplayName("시, 구군, 스토어 아이디, 년도로 정산 데이터 얻기")
  void GetDataBySidoAndGugunAndStoreIdAndYear_whenSpecificationSidoAndGugunAndStoreIdAndYear_getData
      () {
    Specification<Settlement> specification = SettlementSpecification.filterSettlements("SidoB", "GugunB",
        2L, 2023,
        null);

    Page<Settlement> sidoAndGugunAndStoreIdAndYear= repository.findAll(specification, PageRequest.of(0, 10));

    assertEquals(1, sidoAndGugunAndStoreIdAndYear.getTotalElements());
  }


  @DisplayName("시, 구군, 스토어 아이디, 년도, 월로 정산 데이터 얻기")
  void GetDataBySidoAndGugunAndStoreIdAndYearAndMonth_whenSpecificationSidoAndGugunAndStoreIdAndYearAndMonth_getData() {
    Specification<Settlement> specification = SettlementSpecification.filterSettlements("SidoB", "GugunB",
        2L, 2023,
        1);

    Page<Settlement> sidoAndGugunAndStoreIdAndYearAndMonth= repository.findAll(specification, PageRequest.of(0, 10));

    assertEquals(0, sidoAndGugunAndStoreIdAndYearAndMonth.getTotalElements());
  }

  @DisplayName("시, 구군, 년도, 월로 데이터 얻기")
  void GetDataBySidoAndGugunYearAndMonth_whenSpecificationSidoAndGugunAndYearAndMonth_getData() {
    Specification<Settlement> specification = SettlementSpecification.filterSettlements("SidoB", "GugunB",
        2L, 2022,
        1);

    Page<Settlement> sidoAndGugunAndStoreIdAndMonth= repository.findAll(specification, PageRequest.of(0, 10));

    assertEquals(0, sidoAndGugunAndStoreIdAndMonth.getTotalElements());
  }

  @DisplayName("시, 구군, 월 정산 데이터 얻기")
  void GetDataBySidoAndMonth_whenSpecificationSidoAndGugunAndMonth_getData() {
 Specification<Settlement> specification = SettlementSpecification.filterSettlements("SidoB", "GugunB",
        2L, null,
        10);

    Page<Settlement> sidoAndGugunAndStoreIdAndMonth= repository.findAll(specification, PageRequest.of(0, 10));

    assertEquals(1, sidoAndGugunAndStoreIdAndMonth.getTotalElements());
  }

  @DisplayName("스토어아이디로 정산 데이터얻기")
  @Test
  void GetDataByStoreId_whenSpecificationOnlyStoreId_getData() {
    createSettlement(1L, "StoreA", LocalDateTime.of(2023, 11, 15, 12, 30), 1500L, null, null);
    createSettlement(2L, "StoreB", LocalDateTime.of(2023, 10, 10, 9, 0), 2000L, null, null);
    createSettlement(3L, "StoreC", LocalDateTime.of(2023, 10, 10, 9, 0), 2000L, null, null);

    Specification<Settlement> specification = SettlementSpecification.filterSettlements(null, null,
        1L, null,
        null);

    Page<Settlement> storeOnlyResult = repository.findAll(specification, PageRequest.of(0, 10));

    assertEquals(1, storeOnlyResult.getTotalElements());
  }

  @DisplayName("스토어아이디와 년도로 정산 데이터얻기")
  @Test
  void GetDataByStoreIdAndYear_WhenSpecificationOnlyStoreIdAndYear_GetData() {
    createSettlement(1L, "StoreA", LocalDateTime.of(2023, 11, 15, 12, 30), 1500L, null, null);
    createSettlement(2L, "StoreB", LocalDateTime.of(2022, 10, 10, 9, 0), 2000L, null, null);
    createSettlement(2L, "StoreB", LocalDateTime.of(2022, 11, 10, 9, 0), 2000L, null, null);

    Specification<Settlement> storeIdWithYearSpecification = SettlementSpecification.filterSettlements(
        null, null,
        2L, 2022, null);

    Page<Settlement> result = repository.findAll(storeIdWithYearSpecification,
        PageRequest.of(0, 10));

    assertEquals(2L, result.getTotalElements());
  }

  @DisplayName("스토어아이디,년도,월로 정산 데이터얻기")
  @Test
  void GetDataByStoreIdYearAndMonth_WhenSpecificationStoreIdYearAndMonth_GetData() {
    createSettlement(1L, "StoreA", LocalDateTime.of(2023, 11, 15, 12, 30), 1500L, null, null);
    createSettlement(2L, "StoreB", LocalDateTime.of(2022, 10, 10, 9, 0), 2000L, null, null);
    createSettlement(2L, "StoreC", LocalDateTime.of(2022, 2, 10, 9, 0), 2000L, null, null);

    Specification<Settlement> storeIdWithYearSpecification = SettlementSpecification.filterSettlements(
        null, null, 2L, 2022, 2);

    Page<Settlement> result = repository.findAll(storeIdWithYearSpecification,
        PageRequest.of(0, 10));

    assertEquals(1L, result.getTotalElements());
  }

  @DisplayName("년도로 정산 데이터얻기")
  @Test
  void GetDataByYear_WhenSpecificationOnlyYear_GetData() {
    createSettlement(1L, "StoreA", LocalDateTime.of(2023, 11, 15, 12, 30), 1500L, null, null);
    createSettlement(2L, "StoreB", LocalDateTime.of(2023, 10, 10, 9, 0), 2000L, null, null);
    createSettlement(3L, "StoreC", LocalDateTime.of(2023, 10, 10, 9, 0), 2000L, null, null);

    Specification<Settlement> storeIdWithYearSpecification = SettlementSpecification.filterSettlements(
        null, null,
        null, 2022, null);

    Page<Settlement> result = repository.findAll(storeIdWithYearSpecification,
        PageRequest.of(0, 10));

    assertEquals(0L, result.getTotalElements());

  }

  @DisplayName("년도와 월로 정산 데이터 얻기")
  @Test
  void GetDataByYearAndMonth_WhenSpecificationYearAndMonthGetData() {
    createSettlement(1L, "StoreA", LocalDateTime.of(2023, 11, 15, 12, 30), 1500L, null, null);
    createSettlement(2L, "StoreB", LocalDateTime.of(2023, 10, 10, 9, 0), 2000L, null, null);
    createSettlement(3L, "StoreC", LocalDateTime.of(2023, 10, 10, 9, 0), 2000L, null, null);

    Specification<Settlement> storeIdWithYearSpecification = SettlementSpecification.filterSettlements(
        null, null,
        null, 2023, 10);

    Page<Settlement> result = repository.findAll(storeIdWithYearSpecification,
        PageRequest.of(0, 10));

    assertEquals(2L, result.getTotalElements());
  }


  @DisplayName("월만 입력되어있을 때 해당 월에 해당 되는 년도 데이터 모두 받기")
  @Test
  void GetDataAllData_WhenSpecificationOnlyMonth_GetAllData() {
    createSettlement(1L, "StoreA", LocalDateTime.of(2023, 11, 15, 12, 30), 1500L, null, null);
    createSettlement(2L, "StoreB", LocalDateTime.of(2023, 10, 10, 9, 0), 2000L, null, null);
    createSettlement(3L, "StoreC", LocalDateTime.of(2023, 10, 10, 9, 0), 2000L, null, null);
    createSettlement(1L, "StoreD", LocalDateTime.of(2023, 10, 15, 12, 30), 1500L, null, null);
    createSettlement(2L, "StoreE", LocalDateTime.of(2023, 10, 10, 9, 0), 2000L, null, null);
    createSettlement(3L, "StoreF", LocalDateTime.of(2023, 10, 10, 9, 0), 2000L, null, null);

    Specification<Settlement> storeIdWithYearSpecification = SettlementSpecification.filterSettlements(
        null, null, null, null, 10);

    Page<Settlement> result = repository.findAll(storeIdWithYearSpecification,
        PageRequest.of(0, 10));

    assertEquals(5L, result.getTotalElements());
  }

  @DisplayName("Specification 아무것도 없을 때 전체 데이터 받기")
  @Test
  void GetDataByNone_WhenSpecificationNone_GetAllData() {
    createSettlement(1L, "StoreA", LocalDateTime.of(2023, 11, 15, 12, 30), 1500L, null, null);
    createSettlement(2L, "StoreB", LocalDateTime.of(2023, 10, 10, 9, 0), 2000L, null, null);
    createSettlement(3L, "StoreC", LocalDateTime.of(2023, 10, 10, 9, 0), 2000L, null, null);
    createSettlement(1L, "StoreD", LocalDateTime.of(2023, 10, 15, 12, 30), 1500L, null, null);
    createSettlement(2L, "StoreE", LocalDateTime.of(2023, 10, 10, 9, 0), 2000L, null, null);
    createSettlement(3L, "StoreF", LocalDateTime.of(2023, 10, 10, 9, 0), 2000L, null, null);

    Specification<Settlement> storeIdWithYearSpecification = SettlementSpecification.filterSettlements(
        null, null, null, null, null);

    Page<Settlement> result = repository.findAll(storeIdWithYearSpecification,
        PageRequest.of(0, 10));

    assertEquals(6L, result.getTotalElements());
  }

}
