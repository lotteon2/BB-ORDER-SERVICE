package kr.bb.order.controller.restcontroller;

import bloomingblooms.domain.notification.delivery.DeliveryStatus;
import bloomingblooms.domain.notification.order.OrderType;
import bloomingblooms.domain.order.OrderMethod;
import bloomingblooms.domain.payment.KakaopayReadyResponseDto;
import bloomingblooms.response.CommonResponse;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import kr.bb.order.dto.request.orderForDelivery.OrderForDeliveryRequest;
import kr.bb.order.dto.request.orderForPickup.OrderForPickupDto;
import kr.bb.order.dto.request.orderForSubscription.OrderForSubscriptionDto;
import kr.bb.order.dto.response.order.WeeklySalesInfoDto;
import kr.bb.order.dto.response.order.details.OrderDeliveryGroup;
import kr.bb.order.dto.response.order.details.OrderInfoForStoreForSeller;
import kr.bb.order.dto.response.order.list.OrderDeliveryPageInfoDto;
import kr.bb.order.dto.response.order.list.OrderDeliveryPageInfoForSeller;
import kr.bb.order.service.OrderDetailsService;
import kr.bb.order.service.OrderListService;
import kr.bb.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

  @Value("${host.front-url}")
  private String FRONTEND_URL;

  // 바로 주문(배송) 준비 단계
  @PostMapping("/delivery")
  public CommonResponse<KakaopayReadyResponseDto> readyForDirectOrder(
      @RequestHeader Long userId, @RequestBody OrderForDeliveryRequest requestDto) {
    KakaopayReadyResponseDto responseDto =
        orderService.readyForOrder(userId, requestDto, OrderType.DELIVERY, OrderMethod.DIRECT);
    return CommonResponse.success(responseDto);
  }

  // 바로 주문(배송), 픽업, 구독 승인 단계
  @GetMapping("/approve/{partnerOrderId}/{orderType}")
  public void requestOrder(
      @PathVariable("partnerOrderId") String orderId,
      @PathVariable("orderType") String orderType,
      @RequestParam("pg_token") String pgToken,
      HttpServletResponse httpServletResponse)
      throws IOException {
    orderService.requestOrder(orderId, orderType, pgToken);
    httpServletResponse.sendRedirect(String.format("%s/payment/approve", FRONTEND_URL));
  }

  // 결제 준비 상태에서 결제 취소시
  @GetMapping("/cancel")
  public void cancel(HttpServletResponse httpServletResponse) throws IOException {
    httpServletResponse.sendRedirect(String.format("%s/payment/cancel", FRONTEND_URL));
  }

  // '결제승인' 단계에서 카카오페이 쪽의 오류 발생시 (ex. 잔액 부족)
  @GetMapping("/fail")
  public void fail(HttpServletResponse httpServletResponse) throws IOException {
    httpServletResponse.sendRedirect(String.format("%s/payment/fail", FRONTEND_URL));
  }

  // 장바구니에서 주문(배송) 준비 단계
  @PostMapping("/cart")
  public CommonResponse<KakaopayReadyResponseDto> readyForCartOrder(
      @RequestHeader Long userId, @RequestBody OrderForDeliveryRequest requestDto) {

    KakaopayReadyResponseDto kakaopayReadyResponseDto =
        orderService.readyForOrder(userId, requestDto, OrderType.DELIVERY, OrderMethod.CART);
    return CommonResponse.success(kakaopayReadyResponseDto);
  }

  // 픽업 주문 준비 단계
  @PostMapping("/pickup")
  public CommonResponse<KakaopayReadyResponseDto> readyForPickupOrder(
      @RequestHeader Long userId, @RequestBody OrderForPickupDto requestDto) {

    KakaopayReadyResponseDto kakaopayReadyResponseDto =
        orderService.readyForPickupOrder(userId, requestDto, OrderType.PICKUP);
    return CommonResponse.success(kakaopayReadyResponseDto);
  }

  // 구독 주문 준비 단계
  @PostMapping("/subscription")
  public CommonResponse<KakaopayReadyResponseDto> readyForSubscriptionOrder(
      @RequestHeader Long userId, @RequestBody OrderForSubscriptionDto requestDto) {
    KakaopayReadyResponseDto kakaopayReadyResponseDto =
        orderService.readyForSubscriptionOrder(userId, requestDto, OrderType.SUBSCRIBE);
    return CommonResponse.success(kakaopayReadyResponseDto);
  }

  // 회원- 주문(배송) 목록 조회
  @GetMapping("/delivery")
  public CommonResponse<OrderDeliveryPageInfoDto> getOrderDeliveryListForUser(
      @RequestHeader Long userId,
      @PageableDefault(page = 0, size = 5) Pageable pageable,
      @RequestParam("status") String status) {

    DeliveryStatus orderDeliveryStatus = parseOrderDeliveryStatus(status);

    OrderDeliveryPageInfoDto orderDeliveryPageInfoDto =
        orderListService.getUserOrderDeliveryList(userId, pageable, orderDeliveryStatus);
    return CommonResponse.success(orderDeliveryPageInfoDto);
  }

  // 가게- 주문(배송) 목록 조회
  @GetMapping("/store/delivery")
  public CommonResponse<OrderDeliveryPageInfoForSeller> getOrderDeliveryListForSeller(
      @PageableDefault(page = 0, size = 5) Pageable pageable,
      @RequestParam("status") String status,
      @RequestParam("storeId") Long storeId) {

    DeliveryStatus deliveryStatus = parseOrderDeliveryStatus(status);

    OrderDeliveryPageInfoForSeller orderDeliveryPageInfoForSeller =
        orderListService.getOrderDeliveryListForSeller(pageable, deliveryStatus, storeId);
    return CommonResponse.success(orderDeliveryPageInfoForSeller);
  }

  // 회원- 주문(배송) 상세 조회
  @GetMapping("/delivery/details/{orderGroupId}")
  public CommonResponse<OrderDeliveryGroup> getOrderDeliveryDetailsForUser(
      @PathVariable String orderGroupId) {
    return CommonResponse.success(orderDetailsService.getOrderDetailsForUser(orderGroupId));
  }

  // 가게- 주문(배송) 상세 조회
  @GetMapping("/store/delivery/details/{orderDeliveryId}")
  public CommonResponse<OrderInfoForStoreForSeller> getOrderDeliveryDetailsForSeller(
      @PathVariable String orderDeliveryId) {
    return CommonResponse.success(orderDetailsService.getOrderDetailsForSeller(orderDeliveryId));
  }

  @GetMapping("/store/{storeId}/weekly/sales")
  public CommonResponse<WeeklySalesInfoDto> getWeeklySalesInfo(@PathVariable Long storeId) {
    return CommonResponse.success(orderDetailsService.getWeeklySalesInfo(storeId));
  }

  public DeliveryStatus parseOrderDeliveryStatus(String status) {
    try {
      return DeliveryStatus.valueOf(status);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("올바르지 않은 정렬값 입니다: " + status);
    }
  }
}
