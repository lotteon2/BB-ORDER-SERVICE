package kr.bb.order.controller.helper;

import java.util.ArrayList;
import java.util.List;
import kr.bb.order.dto.request.store.StoreDto;
import kr.bb.order.dto.response.settlement.SettlementDto;
import kr.bb.order.dto.response.settlement.SettlementResponse;
import kr.bb.order.entity.settlement.Settlement;
import kr.bb.order.feign.settlement.GetStoreInfoFeignRequest;
import kr.bb.order.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class GetStoreInfoFeignRequestFacade {

  private final SettlementService settlementService;
  private final GetStoreInfoFeignRequest feignRequest;

  public List<StoreDto> handleFeign(int year, int month, Long storeId) {
    List<StoreDto> storeDtoList = new ArrayList<>();
    if (storeId == null) {
      storeDtoList =  feignRequest.getAllStore().getBody();

    } else {
      StoreDto storeDto = feignRequest.getOneStore(storeId).getBody();
      storeDtoList = List.of(storeDto);
    }
      return storeDtoList;
  }

}
