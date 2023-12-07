package kr.bb.order.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import kr.bb.order.dto.kafka.ProcessOrderDto;
import kr.bb.order.dto.request.delivery.DeliveryInsertRequestDto;
import kr.bb.order.dto.request.orderForDelivery.OrderForDeliveryRequest;
import kr.bb.order.dto.request.orderForDelivery.OrderInfoByStore;
import kr.bb.order.dto.request.orderForDelivery.ProductCreate;
import kr.bb.order.dto.request.payment.KakaopayApproveRequestDto;
import kr.bb.order.dto.request.payment.KakaopayReadyRequestDto;
import kr.bb.order.dto.request.product.PriceCheckDto;
import kr.bb.order.dto.request.store.CouponAndDeliveryCheckDto;
import kr.bb.order.dto.response.payment.KakaopayReadyResponseDto;
import kr.bb.order.entity.OrderType;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.entity.redis.OrderInfo;
import kr.bb.order.exception.PaymentExpiredException;
import kr.bb.order.feign.DeliveryServiceClient;
import kr.bb.order.feign.PaymentServiceClient;
import kr.bb.order.feign.ProductServiceClient;
import kr.bb.order.feign.StoreServiceClient;
import kr.bb.order.kafka.KafkaProducer;
import kr.bb.order.repository.OrderDeliveryRepository;
import kr.bb.order.util.OrderUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
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
  private final OrderDeliveryRepository orderDeliveryRepository;
  private final DeliveryServiceClient deliveryServiceClient;
  private final KafkaProducer kafkaProducer;
  private final OrderUtil orderUtil;

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
    String tempOrderId = orderUtil.generateUUID();
    boolean isSubscriptionPay = false;
    KakaopayReadyRequestDto readyRequestDto =
        KakaopayReadyRequestDto.toDto(
            userId,
            tempOrderId,
            OrderType.ORDER_DELIVERY.toString(),
            requestDto,
            isSubscriptionPay);

    // payment-service로 결제 준비 요청
    KakaopayReadyResponseDto responseDto = paymentServiceClient.ready(readyRequestDto).getData();

    // 주문정보와 tid를 redis에 저장
    String itemName = readyRequestDto.getItemName();
    int quantity = readyRequestDto.getQuantity();
    OrderInfo orderInfo =
        OrderInfo.transformDataForApi(
            tempOrderId, userId, itemName, quantity, isSubscriptionPay, responseDto.getTid(), requestDto);
    redisTemplate.opsForValue().set(tempOrderId, orderInfo);

    return responseDto;
  }

  @Transactional
  public void createOrderForDelivery(String orderId, Long userId, String pgToken) {
    // redis에서 정보 가져오기 및 TTL 갱신
    OrderInfo orderInfo = redisTemplate.opsForValue().get(orderId);
    if (orderInfo == null) throw new PaymentExpiredException();

    orderInfo.setPgToken(pgToken);
    redisTemplate.expire(orderId, 300, TimeUnit.SECONDS);

    ProcessOrderDto processOrderDto =
        ProcessOrderDto.toDto(orderId, orderInfo.getOrderInfoByStores());
    kafkaProducer.sendUseCoupon(processOrderDto);

  }

  @Transactional
  public void processOrder(ProcessOrderDto processOrderDto) {
    // redis에서 order 정보 가져오기
    OrderInfo orderInfo = redisTemplate.opsForValue().get(processOrderDto.getOrderGroupId());
    if (orderInfo == null) throw new PaymentExpiredException();

    // delivery-service로 delivery 정보 저장 및 deliveryId 알아내기
    List<DeliveryInsertRequestDto> dtoList = DeliveryInsertRequestDto.toDto(orderInfo);
    List<Long> deliveryIds = deliveryServiceClient.createDelivery(dtoList).getData();

    // payment-service 결제 승인 요청
    KakaopayApproveRequestDto approveRequestDto = KakaopayApproveRequestDto.toDto(orderInfo, OrderType.ORDER_DELIVERY.toString());
    paymentServiceClient.approve(approveRequestDto).getData();

    // 주문상태 완료로 변경
    for (int i = 0; i < deliveryIds.size(); i++) {
      OrderDelivery orderDelivery =
              OrderDelivery.toDto(
                      deliveryIds.get(i), orderInfo.getUserId(),
                      processOrderDto.getOrderGroupId(),
                      orderInfo.getOrderInfoByStores().get(i));
      orderDeliveryRepository.save(orderDelivery);
    }
  }

  public List<PriceCheckDto> createPriceCheckDto(List<OrderInfoByStore> orderInfoByStores) {
    List<PriceCheckDto> list = new ArrayList<>();
    for (OrderInfoByStore orderInfoByStore : orderInfoByStores) {
      for (ProductCreate productCreate : orderInfoByStore.getProducts()) {
        Long productId = productCreate.getProductId();
        Long price = productCreate.getPrice();
        PriceCheckDto dto = PriceCheckDto.toDto(productId, price);
        list.add(dto);
      }
    }
    return list;
  }

  public List<CouponAndDeliveryCheckDto> createCouponAndDeliveryCheckDto(
      List<OrderInfoByStore> orderInfoByStores) {
    List<CouponAndDeliveryCheckDto> list = new ArrayList<>();
    for (OrderInfoByStore orderInfoByStore : orderInfoByStores) {
      CouponAndDeliveryCheckDto dto = CouponAndDeliveryCheckDto.toDto(orderInfoByStore);
      list.add(dto);
    }
    return list;
  }
}
