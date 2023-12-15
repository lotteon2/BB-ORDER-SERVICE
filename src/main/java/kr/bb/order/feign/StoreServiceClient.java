package kr.bb.order.feign;


import bloomingblooms.response.CommonResponse;
import bloomingblooms.response.SuccessResponse;
import java.util.List;
import java.util.Map;
import kr.bb.order.dto.request.store.CouponAndDeliveryCheckDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name ="storeServiceClient")
public interface StoreServiceClient {
    @PostMapping("/store/validate-purhcase")
    CommonResponse<Void> validatePurchaseDetails(@RequestBody List<CouponAndDeliveryCheckDto> dtos);

    @GetMapping("/store/store-name")
    CommonResponse<Map<Long, String>> getStoreName(@RequestParam List<Long> storeIds);
}
