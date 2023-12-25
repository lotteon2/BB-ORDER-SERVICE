package kr.bb.order.controller.helper;

import java.util.ArrayList;
import java.util.List;
import kr.bb.order.dto.request.store.StoreDto;
import kr.bb.order.feign.settlement.GetStoreInfoFeignRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class GetStoreInfoFeignRequestFacade {

  private final GetStoreInfoFeignRequest feignRequest;

  public List<StoreDto> handleFeign(Long storeId) {
    List<StoreDto> storeDtoList;
    if (storeId == null) {
      storeDtoList =  feignRequest.getAllStore().getBody();

    } else {
      StoreDto storeDto = feignRequest.getOneStore(storeId).getBody();
      storeDtoList = List.of(storeDto);
    }
      return storeDtoList;
  }

}
