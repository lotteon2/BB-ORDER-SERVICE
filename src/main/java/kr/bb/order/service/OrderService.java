package kr.bb.order.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import kr.bb.order.dto.request.orderForDelivery.OrderForDeliveryRequest;
import kr.bb.order.dto.request.orderForDelivery.OrderInfoByStore;
import kr.bb.order.dto.request.payment.KakaopayReadyRequestDto;
import kr.bb.order.dto.request.product.PriceCheckDto;
import kr.bb.order.dto.request.store.CouponAndDeliveryCheckDto;
import kr.bb.order.dto.response.payment.KakaopayReadyResponseDto;
import kr.bb.order.entity.OrderType;
import kr.bb.order.entity.redis.OrderInfo;
import kr.bb.order.feign.PaymentServiceClient;
import kr.bb.order.feign.ProductServiceClient;
import kr.bb.order.feign.StoreServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
  private final ProductServiceClient productServiceClient;
  private final StoreServiceClient storeServiceClient;
  private final PaymentServiceClient paymentServiceClient;
  private final RedisTemplate<String, OrderInfo> redisTemplate;
  private final OrderManager orderManager;

  @Transactional
  public KakaopayReadyResponseDto receiveOrderForDelivery(
      Long userId, OrderForDeliveryRequest requestDto) {
    // 결제금액, 재고유무, 쿠폰유효유무 feign을 통해 확인하기

    // product-service로 가격 유효성 확인하기
    List<PriceCheckDto> priceCheckDtos = createPriceCheckDto(requestDto.getOrderInfoByStores());
    productServiceClient.validatePrice(priceCheckDtos).getData();

    // store-service로 쿠폰(가격, 상태), 배송비 정책 확인하기
    List<CouponAndDeliveryCheckDto> couponAndDeliveryCheckDtos =
        createCouponAndDeliveryCheckDto(requestDto.getOrderInfoByStores());
    storeServiceClient.validatePurchaseDetails(couponAndDeliveryCheckDtos).getData();

    // 유효성 검사를 다 통과했다면 이젠 OrderManager를 통해 총 결제 금액이 맞는지 확인하기
    orderManager.checkActualAmountIsValid(
        requestDto.getOrderInfoByStores(), requestDto.getSumOfActualAmount());

    // 임시 주문id 및 결제준비용 dto 생성
    String tempOrderId = UUID.randomUUID().toString();
    KakaopayReadyRequestDto readyRequestDto =
        KakaopayReadyRequestDto.toDto(
            userId, tempOrderId, OrderType.ORDER_DELIVERY.toString(), requestDto);

    // payment-service로 결제 준비 요청
    KakaopayReadyResponseDto responseDto = paymentServiceClient.ready(readyRequestDto).getData();

    // orderInfo에 tid를 담아 redis에 저장
    OrderInfo orderInfo =
        OrderInfo.transformDataForApi(
            tempOrderId,
            readyRequestDto.getItemName(),
            readyRequestDto.getQuantity(),
            responseDto.getTid(),
            requestDto);
    ValueOperations<String, OrderInfo> valueOperations = redisTemplate.opsForValue();
    valueOperations.set(tempOrderId, orderInfo);

    return responseDto;
  }

  public List<PriceCheckDto> createPriceCheckDto(List<OrderInfoByStore> orderInfoByStores) {
    List<PriceCheckDto> list = new ArrayList<>();
    for (int i = 0; i < orderInfoByStores.size(); i++) {
      for (int j = 0; j < orderInfoByStores.get(i).getProducts().size(); j++) {
        Long productId = orderInfoByStores.get(i).getProducts().get(j).getProductId();
        Long price = orderInfoByStores.get(i).getProducts().get(j).getPrice();
        PriceCheckDto dto = PriceCheckDto.toDto(productId, price);
        list.add(dto);
      }
    }
    return list;
  }

  public List<CouponAndDeliveryCheckDto> createCouponAndDeliveryCheckDto(
      List<OrderInfoByStore> orderInfoByStores) {
    List<CouponAndDeliveryCheckDto> list = new ArrayList<>();
    for (int i = 0; i < orderInfoByStores.size(); i++) {
      CouponAndDeliveryCheckDto dto = CouponAndDeliveryCheckDto.toDto(orderInfoByStores.get(i));
      list.add(dto);
    }
    return list;
  }
}
