package kr.bb.order.service.settlement.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;
import kr.bb.order.repository.OrderPickupRepository;
import kr.bb.order.util.StoreIdAndTotalAmountProjection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
public class ProjectionReturnTypeTest {

  @Autowired
  private OrderPickupRepository orderPickupRepository;

  @Test
  void GetProjectionData_WhenRequestToOrderPickUpRepositorySettlemenent_GetProjectionData() {

    LocalDate startDate = LocalDate.of(2022, 1, 1);
    LocalDate endDate = LocalDate.of(2022, 2, 1);

    // When
    List<StoreIdAndTotalAmountProjection> result = orderPickupRepository.findAllStoreIdAndTotalAmountForDateRange(
        startDate, endDate);

    assertEquals(3,
        result.size());
  }


}
