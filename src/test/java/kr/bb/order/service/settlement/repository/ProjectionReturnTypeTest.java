package kr.bb.order.service.settlement.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import kr.bb.order.entity.CardStatus;
import kr.bb.order.entity.OrderPickupProduct;
import kr.bb.order.entity.ReviewStatus;
import kr.bb.order.entity.pickup.OrderPickup;
import kr.bb.order.entity.pickup.OrderPickupStatus;
import kr.bb.order.repository.OrderPickupProductRepository;
import kr.bb.order.repository.OrderPickupRepository;
import kr.bb.order.util.StoreIdAndTotalAmountProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
class ProjectionReturnTypeTest {

  @Autowired
  private OrderPickupRepository orderPickupRepository;


  @BeforeEach
  public void setUp() {
    OrderPickup orderPickup = OrderPickup.builder()
        .orderPickupId("1")
        .userId(123L)
        .storeId(456L)
        .orderPickupStatus(OrderPickupStatus.PENDING)
        .orderPickupTotalAmount(100L)
        .orderPickupCouponAmount(10L)
        .orderPickupIsComplete(false)
        .orderPickupDatetime(LocalDateTime.now())
        .orderPickupProduct(null)
        .build();

    orderPickupRepository.save(orderPickup);

  }


  @Test
  void GetProjectionData_WhenRequestToOrderPickUpRepositorySettlemenent_GetProjectionData() {

    LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
    LocalDateTime endDate = LocalDateTime.of(2024, 1, 13, 0, 0);

    List<StoreIdAndTotalAmountProjection> result = orderPickupRepository.findAllStoreIdAndTotalAmountForDateRange(
        startDate, endDate);


    assertNotNull(result);
    assertEquals(result.size(), 1);
    assertTrue(result.stream()
        .allMatch(projection -> projection instanceof StoreIdAndTotalAmountProjection));


  }


}
