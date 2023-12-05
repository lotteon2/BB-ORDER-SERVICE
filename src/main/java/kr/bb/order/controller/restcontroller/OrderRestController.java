package kr.bb.order.controller.restcontroller;

import kr.bb.order.dto.request.orderForDelivery.OrderForDeliveryRequest;
import kr.bb.order.dto.response.payment.KakaopayReadyResponseDto;
import kr.bb.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class OrderRestController {
  private final OrderService orderService;

  @PostMapping("/orders/delivery")
  public ResponseEntity<KakaopayReadyResponseDto> receiveOrderForDelivery(
      @RequestHeader Long userId, @RequestBody OrderForDeliveryRequest requestDto) {
    KakaopayReadyResponseDto responseDto = orderService.receiveOrderForDelivery(userId, requestDto);
    return ResponseEntity.ok().body(responseDto);
  }


}
