package kr.bb.order.service.settlement;


import java.util.List;
import kr.bb.order.dto.response.settlement.LastMonthTop10SalesResponse;
import kr.bb.order.dto.response.settlement.SettlementDto;
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


  public List<SettlementDto> getSettlement(Long storeId, Integer year, Integer month, int page,
      int size) {
    return getSettlementService.getSettlement(storeId, year, month, page, size);
  }

}
