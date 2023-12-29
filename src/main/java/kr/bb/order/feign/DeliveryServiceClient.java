package kr.bb.order.feign;

import bloomingblooms.domain.delivery.DeliveryAddressInsertDto;
import bloomingblooms.domain.delivery.DeliveryInsertDto;
import bloomingblooms.domain.delivery.DeliveryInfoDto;
import bloomingblooms.response.CommonResponse;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "deliveryServiceClient", url = "${endpoint.delivery-service}")
public interface DeliveryServiceClient {
    @PostMapping(value = "/client/delivery")
    CommonResponse<List<Long>> createDelivery(@RequestBody List<DeliveryInsertDto> dtoList);

    @GetMapping(value = "/client/delivery/requests")  // key: deliveryId
    CommonResponse<Map<Long, DeliveryInfoDto>> getDeliveryInfo(@RequestParam List<Long> deliveryIds);

    @PostMapping(value = "/client/delivery/delivery-address")
    CommonResponse<Void> createDeliveryAddress(@RequestBody DeliveryAddressInsertDto createDeliveryAddress);
}
