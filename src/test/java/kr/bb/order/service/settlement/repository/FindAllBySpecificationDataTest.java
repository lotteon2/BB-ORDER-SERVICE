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

  private void createSettlement(Long storeId, LocalDateTime settlementDate,
      Long settlementAmount) {
    Settlement settlement = Settlement.builder().storeId(storeId).settlementAmount(settlementAmount)
        .settlementDate(settlementDate)
        .build();
    repository.save(settlement);
  }


  @DisplayName("스토어아이디로 정산 데이터얻기")
  @Test
  void getDataByStoreId_whenSpecificationOnlyStoreId_getData()
{
    createSettlement(1L, LocalDateTime.of(2023, 11, 15, 12, 30), 1500L);
    createSettlement(2L, LocalDateTime.of(2023, 10, 10, 9, 0), 2000L);
    createSettlement(3L, LocalDateTime.of(2023, 10, 10, 9, 0), 2000L);

    Specification<Settlement> specification = SettlementSpecification.filterSettlements(1L, null,
        null);

    Page<Settlement> storeOnlyResult = repository.findAll(specification, PageRequest.of(0, 10));

    assertEquals(1, storeOnlyResult.getTotalElements());
  }

  @DisplayName("스토어아이디와 년도로 정산 데이터얻기")
  @Test
  void GetDataByStoreIdAndYear_WhenSpecificationOnlyStoreIdAndYear_GetData()
       {
    createSettlement(1L, LocalDateTime.of(2023, 11, 15, 12, 30), 1500L);
    createSettlement(2L, LocalDateTime.of(2022, 10, 10, 9, 0), 2000L);
    createSettlement(2L, LocalDateTime.of(2022, 12, 5, 9, 0), 2000L);

    Specification<Settlement> storeIdWithYearSpecification = SettlementSpecification.filterSettlements(
        2L, 2022, null);

    Page<Settlement> result = repository.findAll(storeIdWithYearSpecification,
        PageRequest.of(0, 10));

    assertEquals(2L, result.getTotalElements());
  }

  @DisplayName("스토어아이디,년도,월로 정산 데이터얻기")
  @Test
  void GetDataByStoreIdYearAndMonth_WhenSpecificationStoreIdYearAndMonth_GetData()
       {
    createSettlement(1L, LocalDateTime.of(2023, 11, 15, 12, 30), 1500L);
    createSettlement(2L, LocalDateTime.of(2022, 2, 10, 9, 0), 2000L);
    createSettlement(2L, LocalDateTime.of(2022, 12, 5, 9, 0), 2000L);

    Specification<Settlement> storeIdWithYearSpecification = SettlementSpecification.filterSettlements(
        2L, 2022, 2);

    Page<Settlement> result = repository.findAll(storeIdWithYearSpecification,
        PageRequest.of(0, 10));

    assertEquals(1L, result.getTotalElements());
  }

  @DisplayName("년도로 정산 데이터얻기")
  @Test
  void GetDataByYear_WhenSpecificationOnlyYear_GetData()
  {
    createSettlement(1L, LocalDateTime.of(2023, 11, 15, 12, 30), 1500L);
    createSettlement(2L, LocalDateTime.of(2022, 2, 10, 9, 0), 2000L);
    createSettlement(3L, LocalDateTime.of(2022, 12, 5, 9, 0), 2000L);

    Specification<Settlement> storeIdWithYearSpecification = SettlementSpecification.filterSettlements(
        null, 2022, null);

    Page<Settlement> result = repository.findAll(storeIdWithYearSpecification,
        PageRequest.of(0, 10));

    assertEquals(2L, result.getTotalElements());

  }

  @DisplayName("년도와 월로 정산 데이터 얻기")
  @Test
  void GetDataByYearAndMonth_WhenSpecificationYearAndMonthGetData()
       {
    createSettlement(1L, LocalDateTime.of(2023, 11, 15, 12, 30), 1500L);
    createSettlement(2L, LocalDateTime.of(2022, 2, 10, 9, 0), 2000L);
    createSettlement(3L, LocalDateTime.of(2022, 12, 5, 9, 0), 2000L);

    Specification<Settlement> storeIdWithYearSpecification = SettlementSpecification.filterSettlements(
        null, 2022, 12);

    Page<Settlement> result = repository.findAll(storeIdWithYearSpecification,
        PageRequest.of(0, 10));

    assertEquals(1L, result.getTotalElements());
  }


  @DisplayName("월만 입력되어있을 때 전체 받기")
  @Test
  void GetDataAllData_WhenSpecificationOnlyMonth_GetAllData()
       {
    createSettlement(1L, LocalDateTime.of(2023, 11, 15, 12, 30), 1500L);
    createSettlement(2L, LocalDateTime.of(2022, 2, 10, 9, 0), 2000L);
    createSettlement(3L, LocalDateTime.of(2022, 12, 5, 9, 0), 2000L);

    Specification<Settlement> storeIdWithYearSpecification = SettlementSpecification.filterSettlements(
        null, null, 12);

    Page<Settlement> result = repository.findAll(storeIdWithYearSpecification,
        PageRequest.of(0, 10));

    assertEquals(3L, result.getTotalElements());
  }

  @DisplayName("Specification 아무것도 없을 때 전체 데이터 받기")
  @Test
   void GetDataByNone_WhenSpecificationNone_GetAllData()
       {
    createSettlement(1L, LocalDateTime.of(2023, 11, 15, 12, 30), 1500L);
    createSettlement(2L, LocalDateTime.of(2022, 2, 10, 9, 0), 2000L);
    createSettlement(3L, LocalDateTime.of(2022, 12, 5, 9, 0), 2000L);

    Specification<Settlement> storeIdWithYearSpecification = SettlementSpecification.filterSettlements(
        null, null, null);

    Page<Settlement> result = repository.findAll(storeIdWithYearSpecification,
        PageRequest.of(0, 10));

    assertEquals(3L, result.getTotalElements());
  }

}
