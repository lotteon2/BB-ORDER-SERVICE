package kr.bb.order.feign;

import bloomingblooms.domain.order.ValidatePriceDto;
import bloomingblooms.response.CommonResponse;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "storeServiceClient", url="${endpoint.store-service}")
public interface StoreServiceClient {
  @PostMapping("/client/stores/coupons/validate-purchase")
  CommonResponse<Void> validatePurchaseDetails(@RequestBody List<ValidatePriceDto> dtos);

  @GetMapping("/client/stores/store-name")
  CommonResponse<Map<Long, String>> getStoreName(@RequestParam List<Long> storeIds);
}
