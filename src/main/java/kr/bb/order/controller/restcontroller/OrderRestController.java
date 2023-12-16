package kr.bb.order.controller.restcontroller;

import kr.bb.order.dto.request.orderForDelivery.OrderForDeliveryRequest;
import kr.bb.order.dto.response.order.details.OrderDeliveryGroup;
import kr.bb.order.dto.response.order.details.OrderInfoForStoreForSeller;
import kr.bb.order.dto.response.order.list.OrderDeliveryPageInfoDto;
import kr.bb.order.dto.response.order.list.OrderDeliveryPageInfoForSeller;
import kr.bb.order.dto.response.payment.KakaopayReadyResponseDto;
import kr.bb.order.entity.delivery.OrderDeliveryStatus;
import kr.bb.order.service.OrderDetailsService;
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
  private final OrderDetailsService orderDetailsService;

  @PostMapping("/delivery")
  public ResponseEntity<KakaopayReadyResponseDto> receiveOrderForDelivery(
      @RequestHeader Long userId, @RequestBody OrderForDeliveryRequest requestDto) {
    KakaopayReadyResponseDto responseDto = orderService.receiveOrderForDelivery(userId, requestDto);
    return ResponseEntity.ok().body(responseDto);
  }

  @GetMapping("/approve/{partnerOrderId}/{partnerUserId}")
  public ResponseEntity<Void> processOrder(
      @PathVariable("partnerOrderId") String orderId,
      @PathVariable("partnerUserId") Long userId,
      @RequestParam("pg_token") String pgToken) {
    orderService.createOrderForDelivery(orderId, userId, pgToken);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/delivery")
  public ResponseEntity<OrderDeliveryPageInfoDto> getOrderDeliveryListForUser(
      @RequestHeader Long userId, @PageableDefault(page = 0, size = 5) Pageable pageable, @RequestParam("sort") String status) {

    OrderDeliveryStatus orderDeliveryStatus = parseOrderDeliveryStatus(status);

    OrderDeliveryPageInfoDto orderDeliveryPageInfoDto = orderListService.getUserOrderDeliveryList(
            userId, pageable, orderDeliveryStatus);
    return ResponseEntity.ok().body(orderDeliveryPageInfoDto);
  }

  @GetMapping("/store/delivery")
  public ResponseEntity<OrderDeliveryPageInfoForSeller> getOrderDeliveryListForSeller(
      @PageableDefault(page = 0, size = 5) Pageable pageable, @RequestParam("sort") String status, @RequestParam("storeId") Long storeId) {

    OrderDeliveryStatus orderDeliveryStatus = parseOrderDeliveryStatus(status);

    OrderDeliveryPageInfoForSeller orderDeliveryPageInfoForSeller = orderListService.getOrderDeliveryListForSeller(pageable, orderDeliveryStatus, storeId);
    return ResponseEntity.ok().body(orderDeliveryPageInfoForSeller);
  }

  @GetMapping("/delivery/details/{orderGroupId}")
  public ResponseEntity<OrderDeliveryGroup> getOrderDeliveryDetailsForUser(@PathVariable String orderGroupId){
    return ResponseEntity.ok().body(orderDetailsService.getOrderDetailsForUser(
            orderGroupId));
  }

  @GetMapping("/store/delivery/details/{orderDeliveryId}")
  public ResponseEntity<OrderInfoForStoreForSeller> getOrderDeliveryDetailsForSeller(@PathVariable String orderDeliveryId){
    return ResponseEntity.ok().body(orderDetailsService.getOrderDetailsForSeller(orderDeliveryId));
  }

  public OrderDeliveryStatus parseOrderDeliveryStatus(String status) {
    try {
      return OrderDeliveryStatus.valueOf(status);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("올바르지 않은 정렬값 입니다: " + status);
    }
  }

}
