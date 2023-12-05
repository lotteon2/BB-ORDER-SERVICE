package kr.bb.order.feign;

import bloomingblooms.response.CommonResponse;
import java.util.List;
import kr.bb.order.dto.request.product.PriceCheckDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name="productServiceClient", url="${endpoint.product-service}")
public interface ProductServiceClient {
    @PostMapping(value="products/validate-price")
    CommonResponse<Void> validatePrice(@RequestBody List<PriceCheckDto> dtoList);
}
