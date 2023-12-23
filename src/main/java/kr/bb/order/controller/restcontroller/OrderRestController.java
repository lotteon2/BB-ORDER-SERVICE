package kr.bb.order.controller.restcontroller;

import bloomingblooms.domain.payment.KakaopayReadyResponseDto;
import kr.bb.order.dto.request.orderForDelivery.OrderForDeliveryRequest;
import kr.bb.order.dto.request.orderForPickup.OrderForPickupDto;
import kr.bb.order.dto.response.order.WeeklySalesInfoDto;
import kr.bb.order.dto.response.order.details.OrderDeliveryGroup;
import kr.bb.order.dto.response.order.details.OrderInfoForStoreForSeller;
import kr.bb.order.dto.response.order.list.OrderDeliveryPageInfoDto;
import kr.bb.order.dto.response.order.list.OrderDeliveryPageInfoForSeller;
import kr.bb.order.entity.OrderType;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderRestController {
  private final OrderService orderService;
  private final OrderListService orderListService;
  private final OrderDetailsService orderDetailsService;

  // 바로 주문(배송) 준비 단계
  @PostMapping("/delivery")
  public ResponseEntity<KakaopayReadyResponseDto> readyForDirectOrder(
      @RequestHeader Long userId, @RequestBody OrderForDeliveryRequest requestDto) {
    KakaopayReadyResponseDto responseDto =
        orderService.readyForOrder(userId, requestDto, OrderType.ORDER_DELIVERY);
    return ResponseEntity.ok().body(responseDto);
  }

  // 바로 주문(배송) 승인 단계
  @GetMapping("/approve/{partnerOrderId}/{orderType}")
  public ResponseEntity<Void> requestOrder(
      @PathVariable("partnerOrderId") String orderId,
      @PathVariable("orderType") String orderType,
      @RequestParam("pg_token") String pgToken) {
    orderService.requestOrder(orderId, orderType, pgToken);
    return ResponseEntity.ok().build();
  }

  // 장바구니에서 주문(배송) 준비 단계
  @PostMapping("/cart")
  public ResponseEntity<KakaopayReadyResponseDto> readyForCartOrder(
      @RequestHeader Long userId, @RequestBody OrderForDeliveryRequest requestDto) {

    KakaopayReadyResponseDto kakaopayReadyResponseDto =
        orderService.readyForOrder(userId, requestDto, OrderType.ORDER_CART);
    return ResponseEntity.ok().body(kakaopayReadyResponseDto);
  }

  // 픽업 주문 준비 단계
  @PostMapping("/pickup")
  public ResponseEntity<KakaopayReadyResponseDto> readyForPickupOrder(
      @RequestHeader Long userId, @RequestBody OrderForPickupDto requestDto) {

    KakaopayReadyResponseDto kakaopayReadyResponseDto =
        orderService.readyForPickupOrder(userId, requestDto, OrderType.ORDER_PICKUP);
    return ResponseEntity.ok().body(kakaopayReadyResponseDto);
  }

  // 회원- 주문(배송) 목록 조회
  @GetMapping("/delivery")
  public ResponseEntity<OrderDeliveryPageInfoDto> getOrderDeliveryListForUser(
      @RequestHeader Long userId,
      @PageableDefault(page = 0, size = 5) Pageable pageable,
      @RequestParam("sort") String status) {

    OrderDeliveryStatus orderDeliveryStatus = parseOrderDeliveryStatus(status);

    OrderDeliveryPageInfoDto orderDeliveryPageInfoDto =
        orderListService.getUserOrderDeliveryList(userId, pageable, orderDeliveryStatus);
    return ResponseEntity.ok().body(orderDeliveryPageInfoDto);
  }

  // 가게- 주문(배송) 목록 조회
  @GetMapping("/store/delivery")
  public ResponseEntity<OrderDeliveryPageInfoForSeller> getOrderDeliveryListForSeller(
      @PageableDefault(page = 0, size = 5) Pageable pageable,
      @RequestParam("sort") String status,
      @RequestParam("storeId") Long storeId) {

    OrderDeliveryStatus orderDeliveryStatus = parseOrderDeliveryStatus(status);

    OrderDeliveryPageInfoForSeller orderDeliveryPageInfoForSeller =
        orderListService.getOrderDeliveryListForSeller(pageable, orderDeliveryStatus, storeId);
    return ResponseEntity.ok().body(orderDeliveryPageInfoForSeller);
  }

  // 회원- 주문(배송) 상세 조회
  @GetMapping("/delivery/details/{orderGroupId}")
  public ResponseEntity<OrderDeliveryGroup> getOrderDeliveryDetailsForUser(
      @PathVariable String orderGroupId) {
    return ResponseEntity.ok().body(orderDetailsService.getOrderDetailsForUser(orderGroupId));
  }

  // 가게- 주문(배송) 상세 조회
  @GetMapping("/store/delivery/details/{orderDeliveryId}")
  public ResponseEntity<OrderInfoForStoreForSeller> getOrderDeliveryDetailsForSeller(
      @PathVariable String orderDeliveryId) {
    return ResponseEntity.ok().body(orderDetailsService.getOrderDetailsForSeller(orderDeliveryId));
  }

  @GetMapping("/store/{storeId}/weekly/sales")
  public ResponseEntity<WeeklySalesInfoDto> getWeeklySalesInfo(@PathVariable Long storeId){
    return ResponseEntity.ok().body(orderDetailsService.getWeeklySalesInfo(storeId));
  }

  public OrderDeliveryStatus parseOrderDeliveryStatus(String status) {
    try {
      return OrderDeliveryStatus.valueOf(status);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("올바르지 않은 정렬값 입니다: " + status);
    }
  }
  
}
