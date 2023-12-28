package kr.bb.order.controller.helper;

import bloomingblooms.domain.store.StoreInfoDto;
import java.util.List;
import kr.bb.order.feign.settlement.GetStoreInfoFeignRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class GetStoreInfoFeignRequestFacade {

  private final GetStoreInfoFeignRequest feignRequest;

  public List<StoreInfoDto> handleFeign(Long storeId) {
    List<StoreInfoDto> storeInfoDtoList;
    if (storeId == null) {
      storeInfoDtoList =  feignRequest.getAllStore().getData();

    } else {
      StoreInfoDto storeDto = feignRequest.getOneStore(storeId).getData();
      storeInfoDtoList = List.of(storeDto);
    }
      return storeInfoDtoList;
  }

}
