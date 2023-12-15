package kr.bb.order.feign;

import bloomingblooms.response.CommonResponse;
import java.util.List;
import java.util.Map;
import kr.bb.order.dto.request.delivery.DeliveryInsertRequestDto;
import kr.bb.order.dto.response.delivery.DeliveryInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "deliveryServiceClient")
public interface DeliveryServiceClient {
    @PostMapping(value = "/delivery")
    CommonResponse<List<Long>> createDelivery(@RequestBody List<DeliveryInsertRequestDto> dtoList);

    @GetMapping(value = "/delivery/requests")  // key: deliveryId
    CommonResponse<Map<Long, DeliveryInfoDto>> getDeliveryInfo(@RequestParam List<Long> deliveryIds);
}
