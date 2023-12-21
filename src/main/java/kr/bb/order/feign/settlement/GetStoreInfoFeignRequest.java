package kr.bb.order.feign.settlement;


import java.util.List;
import kr.bb.order.dto.request.store.StoreDto;
import org.springframework.cloud.openfeign.FeignClient;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "get-all-store-info", url = "${service.store.domain}")
public interface GetStoreInfoFeignRequest {

  @GetMapping("/client/stores/{storeId}")
  ResponseEntity<StoreDto> getOneStore(@PathVariable Long storeId);

  @GetMapping("/client/stores")
  ResponseEntity<List<StoreDto>> getAllStore();

}
