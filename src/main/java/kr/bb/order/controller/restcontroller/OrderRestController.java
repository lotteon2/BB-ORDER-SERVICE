package kr.bb.order.controller.restcontroller;

import kr.bb.order.dto.request.orderForDelivery.OrderForDeliveryRequest;
import kr.bb.order.dto.response.order.OrderDeliveryPageInfoDto;
import kr.bb.order.dto.response.payment.KakaopayReadyResponseDto;
import kr.bb.order.service.OrderListService;
import kr.bb.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderRestController {
  private final OrderService orderService;
  private final OrderListService orderListService;

  @PostMapping("/delivery")
  public ResponseEntity<KakaopayReadyResponseDto> receiveOrderForDelivery(
      @RequestHeader Long userId, @RequestBody OrderForDeliveryRequest requestDto) {
    KakaopayReadyResponseDto responseDto = orderService.receiveOrderForDelivery(userId, requestDto);
    return ResponseEntity.ok().body(responseDto);
  }

  @GetMapping("/approve/{partnerOrderId}/{partnerUserId}")
  public ResponseEntity<Void> processOrder(
      @PathVariable("partnerOrderId") String orderId, @PathVariable("partnerUserId") Long userId, @RequestParam("pg_token") String pgToken) {
    orderService.createOrderForDelivery(orderId, userId, pgToken);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/delivery")
  public ResponseEntity<OrderDeliveryPageInfoDto> getOrderDeliveryListForUser(@RequestHeader Long userId, @PageableDefault(page = 0, size = 5)
          Pageable pageable){

    OrderDeliveryPageInfoDto orderDeliveryPageInfoDto = orderListService.getOrderDeliveryListForUser(userId, pageable);
    return ResponseEntity.ok().body(orderDeliveryPageInfoDto);
  }
}
