package kr.bb.order.service.settlement;


import kr.bb.order.dto.response.settlement.LastMonthTop10SalesResponse;
import kr.bb.order.dto.response.settlement.SettlementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class SettlementService {

  private final GetTop10SettlementService getTop10SettlementService;
  private final GetSettlementService getSettlementService;

  public LastMonthTop10SalesResponse getTop10() {
    return getTop10SettlementService.getTop10();
  }

  public SettlementResponse getSettlementWithoutLocation( Long storeId, Integer year,
      Integer month, int page,
      int size) {
    return getSettlementService.getSettlement(null,null,storeId, year, month, page, size);
  }


  public SettlementResponse getSettlement(String sido, String gugun, Long storeId, Integer year,
      Integer month, int page,
      int size) {
    return getSettlementService.getSettlement(sido,gugun,storeId, year, month, page, size);
  }

}
