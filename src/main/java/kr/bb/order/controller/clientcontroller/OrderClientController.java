package kr.bb.order.controller.clientcontroller;

import java.time.LocalDateTime;
import kr.bb.order.entity.pickup.OrderPickupStatus;
import kr.bb.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/client")
@RequiredArgsConstructor
public class OrderClientController {
    private final OrderService orderService;

    @GetMapping("/change")
    public void updatePickupStatus() {
        LocalDateTime now = LocalDateTime.now();
        orderService.pickupStatusChange(now, OrderPickupStatus.COMPLETED);
    }
}
