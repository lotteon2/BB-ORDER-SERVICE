package kr.bb.order.feign;

import bloomingblooms.response.SuccessResponse;
import java.util.List;
import kr.bb.order.dto.request.delivery.DeliveryInsertRequestDto;
import kr.bb.order.dto.request.payment.KakaopayReadyRequestDto;
import kr.bb.order.entity.redis.OrderInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "deliveryServiceClient", url = "${endpoint.delivery-service}")
public interface DeliveryServiceClient {
    @PostMapping(value = "/delivery")
    SuccessResponse<List<Long>> createDelivery(@RequestBody List<DeliveryInsertRequestDto> dtoList);
}
