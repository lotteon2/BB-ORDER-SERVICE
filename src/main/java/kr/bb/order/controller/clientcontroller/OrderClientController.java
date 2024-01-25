package kr.bb.order.controller.clientcontroller;

import bloomingblooms.response.CommonResponse;
import java.time.LocalDateTime;
import kr.bb.order.entity.pickup.OrderPickupStatus;
import kr.bb.order.facade.OrderFacade;
import kr.bb.order.service.OrderDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/client")
@RequiredArgsConstructor
public class OrderClientController {
  private final OrderFacade orderFacade;
  private final OrderDetailsService orderDetailsService;

  @GetMapping("/change")
  public void updatePickupStatus() {
    LocalDateTime now = LocalDateTime.now();
    orderFacade.pickupStatusChange(now, OrderPickupStatus.COMPLETED);
  }

  @GetMapping("/{orderId}/delivery-id")
  CommonResponse<Long> getDeliveryId(@PathVariable String orderId) {
    return CommonResponse.success(orderDetailsService.getDeliveryId(orderId));
  }
}
