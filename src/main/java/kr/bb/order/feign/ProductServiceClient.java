package kr.bb.order.feign;

import bloomingblooms.response.CommonResponse;
import java.util.List;
import kr.bb.order.dto.request.product.PriceCheckDto;
import kr.bb.order.dto.request.product.ProductInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name="productServiceClient")
public interface ProductServiceClient {
    @PostMapping(value="products/validate-price")
    CommonResponse<Void> validatePrice(@RequestBody List<PriceCheckDto> dtoList);

    @GetMapping(value="products/product-info")
    CommonResponse<List<ProductInfoDto>> getProductInfo(@RequestBody List<String> productIds);
}
