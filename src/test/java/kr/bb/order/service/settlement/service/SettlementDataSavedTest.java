package kr.bb.order.service.settlement.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bloomingblooms.response.CommonResponse;
import java.util.Arrays;
import java.util.List;
import kr.bb.order.dto.request.settlement.SettlementStoreInfoResponse;
import kr.bb.order.feign.StoreServiceClient;
import kr.bb.order.repository.OrderDeliveryRepository;
import kr.bb.order.repository.OrderPickupRepository;
import kr.bb.order.repository.OrderSubscriptionRepository;
import kr.bb.order.repository.settlement.SettlementJpaRepository;
import kr.bb.order.service.settlement.ProcessingSettlementDataService;
import kr.bb.order.service.settlement.SettlementDataHandler;
import kr.bb.order.util.StoreIdAndTotalAmountProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettlementDataSavedTest {

  @Mock
  OrderDeliveryRepository orderDeliveryRepository;

  @Mock
  OrderPickupRepository orderPickupRepository;

  @Mock
  OrderSubscriptionRepository orderSubscriptionRepository;

  @Mock
  ProcessingSettlementDataService processingSettlementDataService;

  @InjectMocks
  SettlementDataHandler settlementHandler;

  @Mock
  SettlementJpaRepository settlementJpaRepository;

  @Mock
  StoreServiceClient storeServiceClient;



@Test
void testSettlementSavedWhenFeignIsSucceededSavedEntity() {
    List<StoreIdAndTotalAmountProjection> totalAmountByStoreId = Arrays.asList(
        new StoreIdAndTotalAmountProjection(1L, 100L),
        new StoreIdAndTotalAmountProjection(2L, 200L)
    );
    when(processingSettlementDataService.getTotalAmountByStoreId(any(), any())).thenReturn(
        totalAmountByStoreId);

    List<Long> storeIdList = List.of(1L, 2L);

    List<SettlementStoreInfoResponse> settlementStoreInfoResponseList = Arrays.asList(
        new SettlementStoreInfoResponse(1L, "Store1", "Bank1", "Account1", "Gugun1", "Sido1"),
        new SettlementStoreInfoResponse(2L, "Store2", "Bank2", "Account2", "Gugun2", "Sido2")
    );

    when(storeServiceClient.getStoreInfoByStoreId(storeIdList))
        .thenReturn(CommonResponse.<List<SettlementStoreInfoResponse>>builder()
            .data(settlementStoreInfoResponseList).build());

    settlementHandler.saveSettlement();

    verify(settlementJpaRepository, times(1)).saveAll(anyList());

}
}