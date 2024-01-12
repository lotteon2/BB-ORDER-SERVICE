package kr.bb.order.controller.clientcontroller;

import bloomingblooms.domain.StatusChangeDto;
import kr.bb.order.kafka.KafkaProducer;
import kr.bb.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/client")
@RequiredArgsConstructor
public class OrderClientController {
    private final OrderService orderService;

    @GetMapping("/change")
    public void updatePickupStatus() {
        LocalDateTime now = LocalDateTime.now();
        orderService.pickupStatusChange(now);
    }
}
