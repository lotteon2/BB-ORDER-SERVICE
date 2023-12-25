package kr.bb.order.service.settlement.repository;


import kr.bb.order.repository.settlement.SettlementJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class GetTop10SettlementServiceRepositoryTest {

  @Autowired
  private SettlementJpaRepository repository;

  @DisplayName("데이터 DESC 내림차순 테스트")
  @Test
  void GetTop10Data_WhenDataIsExisted_GetDataByDescOrder() {

  }


  @DisplayName("데이터 DESC 내림차순 테스트")
  @Test
  void GetTop10Data_WhenDataIsExisted_GetDataByDescOrder() {

  }


}
