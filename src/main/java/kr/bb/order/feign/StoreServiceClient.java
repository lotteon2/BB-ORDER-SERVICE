package kr.bb.order.feign;


import bloomingblooms.response.CommonResponse;
import bloomingblooms.response.SuccessResponse;
import java.util.List;
import kr.bb.order.dto.request.store.CouponAndDeliveryCheckDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name ="storeServiceClient", url="${endpoint.store-service}")
public interface StoreServiceClient {
    @PostMapping("/store/validate-purhcase")
    CommonResponse<Void> validatePurchaseDetails(@RequestBody List<CouponAndDeliveryCheckDto> dtos);
}
