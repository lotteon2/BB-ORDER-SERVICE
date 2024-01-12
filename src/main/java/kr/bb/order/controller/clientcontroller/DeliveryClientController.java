package kr.bb.order.controller.clientcontroller;

import bloomingblooms.response.CommonResponse;
import kr.bb.order.service.OrderDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/client")
public class DeliveryClientController {
  private final OrderDetailsService orderDetailsService;

  @GetMapping("/{orderId}/delivery-id")
  CommonResponse<Long> getDeliveryId(@PathVariable String orderId) {
    return CommonResponse.success(orderDetailsService.getDeliveryId(orderId));
  }
}
