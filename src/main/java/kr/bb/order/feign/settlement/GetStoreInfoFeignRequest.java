package kr.bb.order.feign.settlement;


import bloomingblooms.domain.store.StoreInfoDto;
import bloomingblooms.response.CommonResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "get-all-store-info", url = "${endpoint.store-service}")
public interface GetStoreInfoFeignRequest {

  @GetMapping("/client/stores/{storeId}")
  CommonResponse<StoreInfoDto> getOneStore(@PathVariable Long storeId);

  @GetMapping("/client/stores")
  CommonResponse<List<StoreInfoDto>> getAllStore();

}
