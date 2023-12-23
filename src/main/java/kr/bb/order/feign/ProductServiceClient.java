package kr.bb.order.feign;

import bloomingblooms.domain.product.IsProductPriceValid;
import bloomingblooms.domain.product.ProductInformation;
import bloomingblooms.response.CommonResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name="productServiceClient")
public interface ProductServiceClient {
    @PostMapping(value="products/validate-price")
    CommonResponse<Void> validatePrice(@RequestBody List<IsProductPriceValid> dtoList);

    @GetMapping(value="products/product-info")
    CommonResponse<List<ProductInformation>> getProductInfo(@RequestBody List<String> productIds);
}
