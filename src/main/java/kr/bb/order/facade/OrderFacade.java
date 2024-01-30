package kr.bb.order.facade;

import static bloomingblooms.domain.notification.delivery.DeliveryStatus.COMPLETED;
import static bloomingblooms.domain.notification.delivery.DeliveryStatus.PROCESSING;

import bloomingblooms.domain.batch.SubscriptionBatchDtoList;
import bloomingblooms.domain.delivery.DeliveryAddressInsertDto;
import bloomingblooms.domain.delivery.DeliveryInsertDto;
import bloomingblooms.domain.delivery.UpdateOrderStatusDto;
import bloomingblooms.domain.delivery.UpdateOrderSubscriptionStatusDto;
import bloomingblooms.domain.notification.order.OrderType;
import bloomingblooms.domain.order.NewOrderEvent;
import bloomingblooms.domain.order.OrderInfoByStore;
import bloomingblooms.domain.order.OrderMethod;
import bloomingblooms.domain.order.PickupStatusChangeDto;
import bloomingblooms.domain.order.ProcessOrderDto;
import bloomingblooms.domain.order.ProductCreate;
import bloomingblooms.domain.order.SubscriptionStatusChangeDto;
import bloomingblooms.domain.order.ValidatePolicyDto;
import bloomingblooms.domain.order.ValidatePriceDto;
import bloomingblooms.domain.payment.KakaopayApproveRequestDto;
import bloomingblooms.domain.payment.KakaopayReadyRequestDto;
import bloomingblooms.domain.payment.KakaopayReadyResponseDto;
import bloomingblooms.domain.pickup.PickupCreateDto;
import bloomingblooms.domain.product.IsProductPriceValid;
import bloomingblooms.domain.review.ReviewStatus;
import bloomingblooms.domain.subscription.SubscriptionCreateDto;
import bloomingblooms.domain.subscription.SubscriptionDateDto;
import bloomingblooms.dto.command.CartDeleteCommand;
import bloomingblooms.dto.command.CartDeleteDto;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import kr.bb.order.dto.request.orderForDelivery.OrderForDeliveryRequest;
import kr.bb.order.dto.request.orderForPickup.OrderForPickupDto;
import kr.bb.order.dto.request.orderForSubscription.OrderForSubscriptionDto;
import kr.bb.order.entity.OrderDeliveryProduct;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.entity.delivery.OrderGroup;
import kr.bb.order.entity.pickup.OrderPickup;
import kr.bb.order.entity.pickup.OrderPickupStatus;
import kr.bb.order.entity.redis.OrderInfo;
import kr.bb.order.entity.redis.PickupOrderInfo;
import kr.bb.order.entity.redis.SubscriptionOrderInfo;
import kr.bb.order.entity.subscription.OrderSubscription;
import kr.bb.order.entity.subscription.SubscriptionStatus;
import kr.bb.order.exception.PaymentExpiredException;
import kr.bb.order.feign.FeignHandler;
import kr.bb.order.infra.OrderSNSPublisher;
import kr.bb.order.infra.OrderSQSPublisher;
import kr.bb.order.kafka.KafkaProducer;
import kr.bb.order.kafka.SubscriptionDateDtoList;
import kr.bb.order.mapper.DeliveryAddressMapper;
import kr.bb.order.mapper.KakaopayMapper;
import kr.bb.order.mapper.OrderCommonMapper;
import kr.bb.order.service.OrderManager;
import kr.bb.order.service.OrderService;
import kr.bb.order.util.OrderUtil;
import kr.bb.order.util.RedisOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderFacade<T> {
  private final RedisOperation redisOperation;

  private final FeignHandler feignHandler;
  private final OrderManager orderManager;

  private final KafkaProducer<ProcessOrderDto> processOrderDtoKafkaProducer;
  private final KafkaProducer<CartDeleteCommand> cartItemDeleteKafkaProducer;
  private final KafkaProducer<PickupCreateDto> pickupCreateDtoKafkaProducer;
  private final KafkaProducer<SubscriptionCreateDto> subscriptionCreateDtoKafkaProducer;
  private final KafkaProducer<SubscriptionStatusChangeDto> subscriptionStatusChangeDtoKafkaProducer;
  private final KafkaProducer<SubscriptionDateDtoList> subscriptionDateDtoListKafkaProducer;
  private final KafkaProducer<PickupStatusChangeDto> pickupStatusUpdateKafkaProducer;

  private final OrderService orderService;

  private final OrderSNSPublisher orderSNSPublisher;
  private final OrderSQSPublisher orderSQSPublisher;

  // 바로 주문 / 장바구니 주문 준비 단계
  public KakaopayReadyResponseDto readyForOrder(
      Long userId,
      OrderForDeliveryRequest requestDto,
      OrderType orderType,
      OrderMethod orderMethod) {

    // product-service로 가격 유효성 확인하기
    List<IsProductPriceValid> priceCheckDtos =
        createPriceCheckDto(requestDto.getOrderInfoByStores());
    feignHandler.validatePrice(priceCheckDtos);

    // store-service로 쿠폰(가격, 상태), 배송비 정책 확인하기
    List<ValidatePriceDto> validatePriceDtos =
        createCouponAndDeliveryCheckDto(requestDto.getOrderInfoByStores());
    ValidatePolicyDto validatePolicyDto =
        ValidatePolicyDto.builder()
            .validatePriceDtos(validatePriceDtos)
            .orderType(OrderType.DELIVERY)
            .build();
    feignHandler.validatePurchaseDetails(validatePolicyDto);

    // 유효성 검사를 다 통과했다면 이젠 OrderManager를 통해 총 결제 금액이 맞는지 확인하기
    orderManager.checkActualAmountIsValid(
        requestDto.getOrderInfoByStores(), requestDto.getSumOfActualAmount());

    // 임시 주문id 및 결제준비용 dto 생성
    String tempOrderId = OrderUtil.generateUUID();
    boolean isSubscriptionPay = false;

    KakaopayReadyRequestDto readyRequestDto =
        KakaopayReadyRequestDto.toDto(
            userId,
            tempOrderId,
            orderType.toString(),
            requestDto.getOrderInfoByStores(),
            requestDto.getSumOfActualAmount(),
            isSubscriptionPay);

    // payment-service로 결제 준비 요청
    KakaopayReadyResponseDto responseDto = feignHandler.ready(readyRequestDto);

    // 주문정보와 tid를 redis에 저장
    String itemName = readyRequestDto.getItemName();
    int quantity = readyRequestDto.getQuantity();
    OrderInfo orderInfo =
        OrderInfo.convertToRedisDto(
            tempOrderId,
            userId,
            itemName,
            quantity,
            isSubscriptionPay,
            responseDto.getTid(),
            requestDto,
            orderType,
            orderMethod);

    redisOperation.saveIntoRedis(tempOrderId, orderInfo);

    return responseDto;
  }

// 픽업 주문 준비 단계
  public KakaopayReadyResponseDto readyForPickupOrder(
      Long userId, OrderForPickupDto requestDto, OrderType orderType) {

    List<OrderInfoByStore> orderInfoByStores = new ArrayList<>();
    OrderInfoByStore orderInfoByStore = createOrderInfoByStore(requestDto);
    orderInfoByStores.add(orderInfoByStore);

    // product-service로 가격 유효성 확인하기
    List<IsProductPriceValid> priceCheckDtos = createPriceCheckDto(orderInfoByStores);
    feignHandler.validatePrice(priceCheckDtos);

    // store-service로 쿠폰(가격, 상태), 배송비 정책 확인하기
    List<ValidatePriceDto> validatePriceDtos = createCouponAndDeliveryCheckDto(orderInfoByStores);
    ValidatePolicyDto validatePolicyDto =
        ValidatePolicyDto.builder()
            .validatePriceDtos(validatePriceDtos)
            .orderType(OrderType.PICKUP)
            .build();

    feignHandler.validatePurchaseDetails(validatePolicyDto);

    // 유효성 검사를 다 통과했다면 이젠 OrderManager를 통해 총 결제 금액이 맞는지 확인하기
    orderManager.checkActualAmountIsValid(orderInfoByStores, requestDto.getActualAmount());

    // 임시 주문id 및 결제준비용 dto 생성
    String tempOrderId = OrderUtil.generateUUID();
    boolean isSubscriptionPay = false;

    KakaopayReadyRequestDto readyRequestDto =
        KakaopayReadyRequestDto.toDto(
            userId,
            tempOrderId,
            orderType.toString(),
            orderInfoByStores,
            requestDto.getActualAmount(),
            isSubscriptionPay);

    // payment-service로 결제 준비 요청
    KakaopayReadyResponseDto responseDto = feignHandler.ready(readyRequestDto);

    // 주문정보와 tid를 redis에 저장
    String itemName = readyRequestDto.getItemName();
    long quantity = readyRequestDto.getQuantity();
    String tid = responseDto.getTid();
    PickupOrderInfo pickupOrderInfo =
        PickupOrderInfo.convertToRedisDto(
            tempOrderId, userId, itemName, quantity, isSubscriptionPay, tid, requestDto, orderType);

    redisOperation.saveIntoRedis(tempOrderId, pickupOrderInfo);

    return responseDto;
  }

  // 구독 주문 준비 단계
  public KakaopayReadyResponseDto readyForSubscriptionOrder(
      Long userId, OrderForSubscriptionDto requestDto, OrderType orderType) {
    OrderInfoByStore orderInfoByStore = createOrderInfoByStoreForSubscription(requestDto);
    List<OrderInfoByStore> orderInfoByStores = List.of(orderInfoByStore);

    // product-service로 가격 유효성 확인하기
    List<IsProductPriceValid> priceCheckDtos = createPriceCheckDto(orderInfoByStores);
    feignHandler.validatePrice(priceCheckDtos);

    // store-service로 쿠폰(가격, 상태), 배송비 정책 확인하기
    List<ValidatePriceDto> validatePriceDtos = createCouponAndDeliveryCheckDto(orderInfoByStores);

    ValidatePolicyDto validatePolicyDto =
        ValidatePolicyDto.builder()
            .validatePriceDtos(validatePriceDtos)
            .orderType(OrderType.SUBSCRIBE)
            .build();

    feignHandler.validatePurchaseDetails(validatePolicyDto);

    // 유효성 검사를 다 통과했다면 이젠 OrderManager를 통해 총 결제 금액이 맞는지 확인하기
    orderManager.checkActualAmountIsValid(orderInfoByStores, requestDto.getActualAmount());

    // 임시 주문id 및 결제준비용 dto 생성
    String tempOrderId = OrderUtil.generateUUID();
    Boolean isSubscriptionPay = true;

    KakaopayReadyRequestDto readyRequestDto =
        KakaopayReadyRequestDto.toDto(
            userId,
            tempOrderId,
            orderType.toString(),
            orderInfoByStores,
            requestDto.getActualAmount(),
            isSubscriptionPay);

    // payment-service로 결제 준비 요청
    KakaopayReadyResponseDto responseDto = feignHandler.ready(readyRequestDto);

    // 주문정보와 tid를 redis에 저장
    String itemName = readyRequestDto.getItemName();
    long quantity = readyRequestDto.getQuantity();
    String tid = responseDto.getTid();
    SubscriptionOrderInfo subscriptionOrderInfo =
        SubscriptionOrderInfo.convertToRedisDto(
            tempOrderId, userId, itemName, quantity, isSubscriptionPay, tid, requestDto, orderType);

    redisOperation.saveIntoRedis(tempOrderId, subscriptionOrderInfo);

    return responseDto;
  }

  // (바로주문, 장바구니 / 픽업주문 / 구독주문) 타 서비스로 kafka 주문 요청 처리 [쿠폰사용, 재고차감]
  // TODO: 추상화를 통해 코드를 줄일 수 없을까?
  public void requestOrder(String orderId, String orderType, String pgToken) {
    // redis에서 정보 가져오기 및 TTL 갱신
    if (orderType.equals(OrderType.DELIVERY.toString())) {
      OrderInfo orderInfo = redisOperation.findFromRedis(orderId, OrderInfo.class);
      if (orderInfo == null) throw new PaymentExpiredException();

      orderInfo.setPgToken(pgToken);
      redisOperation.saveIntoRedis(orderId, orderInfo);
      redisOperation.expire(orderId, 5, TimeUnit.MINUTES);

      ProcessOrderDto processOrderDto =
              OrderCommonMapper.toProcessOrderDto(orderId, orderType, orderInfo);
      processOrderDtoKafkaProducer.send("coupon-use", processOrderDto);
    } else if (orderType.equals(OrderType.PICKUP.toString())) {
      PickupOrderInfo pickupOrderInfo = redisOperation.findFromRedis(orderId, PickupOrderInfo.class);
      if (pickupOrderInfo == null) throw new PaymentExpiredException();

      pickupOrderInfo.setPgToken(pgToken);
      redisOperation.saveIntoRedis(orderId, pickupOrderInfo);
      redisOperation.expire(orderId, 5, TimeUnit.MINUTES);

      ProcessOrderDto processOrderDto =
              OrderCommonMapper.toDtoForOrderPickup(orderId, pickupOrderInfo);
      processOrderDtoKafkaProducer.send("coupon-use", processOrderDto);
    } else {
      SubscriptionOrderInfo subscriptionOrderInfo =
              redisOperation.findFromRedis(orderId, SubscriptionOrderInfo.class);
      if (subscriptionOrderInfo == null) throw new PaymentExpiredException();

      subscriptionOrderInfo.setPgToken(pgToken);
      redisOperation.saveIntoRedis(orderId, subscriptionOrderInfo);
      redisOperation.expire(orderId, 5, TimeUnit.MINUTES);

      ProcessOrderDto processOrderDto =
              OrderCommonMapper.toDtoForOrderSubscription(orderId, subscriptionOrderInfo);
      processOrderDtoKafkaProducer.send("coupon-use", processOrderDto);
    }
  }

  // 주문 저장하기
  public void processOrder(ProcessOrderDto processOrderDto){
    String orderMethod = processOrderDto.getOrderMethod();
    String orderType = processOrderDto.getOrderType();
    if (orderType.equals(OrderType.DELIVERY.toString())) {
      OrderInfo orderInfo = redisOperation.findFromRedis(processOrderDto.getOrderId(), OrderInfo.class);
      if (orderInfo == null) throw new PaymentExpiredException();

      // delivery-service로 delivery 정보 저장 및 deliveryId 알아내기
      List<DeliveryInsertDto> dtoList = OrderCommonMapper.toDeliveryInsertDto(orderInfo);
      List<Long> deliveryIds = feignHandler.createDelivery(dtoList);

      // payment-service 최종 결제 승인 요청
      KakaopayApproveRequestDto approveRequestDto = KakaopayMapper.toDtoFromOrderInfo(orderInfo);
      LocalDateTime paymentDateTime = feignHandler.approve(approveRequestDto);

      // facade layer를 사용함으로써 기존 내부호출 문제 해결
      OrderGroup orderGroup = orderService.createOrderDelivery(deliveryIds, processOrderDto, orderInfo);

      // 장바구니에서 주문이면 장바구니에서 해당 상품들 비우는 kafka 요청
      if (orderInfo.getOrderMethod().equals(OrderMethod.CART.toString())) {
        List<String> productIds =
                orderInfo.getOrderInfoByStores().stream()
                        .flatMap(orderInfoByStore -> orderInfoByStore.getProducts().stream())
                        .map(ProductCreate::getProductId)
                        .collect(Collectors.toList());

        List<CartDeleteDto> cartDeleteDtos = new ArrayList<>();
        for (String productId : productIds) {
          cartDeleteDtos.add(
                  CartDeleteDto.builder().productId(productId).userId(orderInfo.getUserId()).build());
        }

        CartDeleteCommand cartDeleteCommand =
                CartDeleteCommand.builder().cartDeleteDtoList(cartDeleteDtos).build();
        cartItemDeleteKafkaProducer.send("cart-delete", cartDeleteCommand);
      }

      // delivery-service로 신규 배송지 추가 / 기존 배송지 날짜 update하기
      DeliveryAddressInsertDto deliveryAddressInsertDto =
              DeliveryAddressMapper.toDto(
                      orderInfo.getDeliveryAddressId(),
                      orderInfo.getUserId(),
                      orderInfo.getRecipientName(),
                      orderInfo.getDeliveryZipcode(),
                      orderInfo.getDeliveryRoadName(),
                      orderInfo.getDeliveryAddressDetail(),
                      orderInfo.getOrdererPhoneNumber());

      feignHandler.createDeliveryAddress(deliveryAddressInsertDto);

      // SNS로 신규 주문 발생 이벤트 보내기
      NewOrderEvent newOrderEvent =
              OrderCommonMapper.createNewOrderEventListForDelivery(orderGroup, orderInfo);

      orderSNSPublisher.newOrderEventPublish(newOrderEvent);

      // SQS로 고객에게 신규 주문 알리기
      orderSQSPublisher.publishOrderSuccess(orderInfo.getUserId(), orderInfo.getOrdererPhoneNumber());
    }
    else if (orderType.equals(OrderType.PICKUP.toString())) {
      PickupOrderInfo pickupOrderInfo = redisOperation.findFromRedis(processOrderDto.getOrderId(), PickupOrderInfo.class);
      if (pickupOrderInfo == null) throw new PaymentExpiredException();

      // payment-service 최종 결제 승인 요청
      KakaopayApproveRequestDto approveRequestDto =
              KakaopayMapper.toDtoFromPickupOrderInfo(pickupOrderInfo);
      LocalDateTime paymentDateTime = feignHandler.approve(approveRequestDto);

      OrderPickup orderPickup = orderService.createOrderPickup(processOrderDto, pickupOrderInfo);

      // order-query 서비스로 픽업 주문 Kafka send
      PickupCreateDto pickupCreateDto =
              OrderCommonMapper.toPickupCreateDto(pickupOrderInfo, paymentDateTime, orderPickup.getOrderPickupProduct());
      pickupCreateDtoKafkaProducer.send("pickup-create", pickupCreateDto);

      // SNS로 신규 주문 발생 이벤트 보내기
      NewOrderEvent newOrderEvent =
              OrderCommonMapper.createNewOrderEventListForPickup(orderPickup, pickupOrderInfo);
      orderSNSPublisher.newOrderEventPublish(newOrderEvent);

      // SQS로 고객에게 신규 주문 알리기
      orderSQSPublisher.publishOrderSuccess(
              pickupOrderInfo.getUserId(), pickupOrderInfo.getOrdererPhoneNumber());
    }
    else {
      SubscriptionOrderInfo subscriptionOrderInfo = redisOperation.findFromRedis(processOrderDto.getOrderId(), SubscriptionOrderInfo.class);
      if (subscriptionOrderInfo == null) throw new PaymentExpiredException();

      // delivery-service로 delivery 정보 저장 및 deliveryId 알아내기
      List<DeliveryInsertDto> dtoList =
              OrderCommonMapper.toDeliveryInsertDtoForSubscription(subscriptionOrderInfo);

      List<Long> deliveryIds = feignHandler.createDelivery(dtoList);

      // payment-service 최종 결제 승인 요청
      KakaopayApproveRequestDto approveRequestDto =
              KakaopayMapper.toDtoFromSubscriptionOrderInfo(subscriptionOrderInfo, deliveryIds);
      LocalDateTime paymentDateTime = feignHandler.approve(approveRequestDto);

      OrderSubscription orderSubscription =
              orderService.createOrderSubscription(deliveryIds, processOrderDto, subscriptionOrderInfo);

      // order-query 서비스로 구독 주문 Kafka send
      SubscriptionCreateDto subscriptionCreateDto =
              OrderCommonMapper.toSubscriptionCreateDto(
                      subscriptionOrderInfo, paymentDateTime, orderSubscription);
      subscriptionCreateDtoKafkaProducer.send("subscription-create", subscriptionCreateDto);

      // delivery-service로 신규 배송지 추가 / 기존 배송지 날짜 update하기
      DeliveryAddressInsertDto deliveryAddressInsertDto =
              DeliveryAddressMapper.toDto(
                      subscriptionOrderInfo.getDeliveryAddressId(),
                      subscriptionOrderInfo.getUserId(),
                      subscriptionOrderInfo.getRecipientName(),
                      subscriptionOrderInfo.getDeliveryZipcode(),
                      subscriptionOrderInfo.getDeliveryRoadName(),
                      subscriptionOrderInfo.getDeliveryAddressDetail(),
                      subscriptionOrderInfo.getOrdererPhoneNumber());

      feignHandler.createDeliveryAddress(deliveryAddressInsertDto);

      // SNS로 신규 주문 발생 이벤트 보내기
      NewOrderEvent newOrderEvent =
              OrderCommonMapper.createNewOrderEventListForSubscription(
                      orderSubscription, subscriptionOrderInfo);
      orderSNSPublisher.newOrderEventPublish(newOrderEvent);

      // SQS로 고객에게 신규 주문 알리기
      orderSQSPublisher.publishOrderSuccess(
              subscriptionOrderInfo.getUserId(), subscriptionOrderInfo.getOrdererPhoneNumber());
    }
  }

  public void updateOrderDeliveryStatus(UpdateOrderStatusDto statusDto ){
    OrderDelivery orderDelivery = orderService.readOrderDeliveryForStatusUpdate(statusDto);

    // 배송상태가 완료가 된 경우
    if (COMPLETED.equals(statusDto.getDeliveryStatus())) {
      orderDelivery
              .getOrderDeliveryProducts()
              .forEach(OrderDeliveryProduct::updateReviewAndCardStatus);
    }

    // 배송상태가 배송중인 경우
    if (PROCESSING.equals(statusDto.getDeliveryStatus())) {
      orderSQSPublisher.publishDeliveryNotification(
              orderDelivery.getOrderGroup().getUserId(), statusDto.getPhoneNumber());
    }
  }

  public void updateOrderSubscriptionStatus(UpdateOrderSubscriptionStatusDto updateOrderSubscriptionStatusDto ){
    List<OrderSubscription> orderSubscriptions = orderService.readOrderSubscriptionsForStatusChange(
            updateOrderSubscriptionStatusDto);

    // 배송상태를 완료로 변경
    for (OrderSubscription orderSubscription : orderSubscriptions) {
      orderSubscription.updateReviewStatus(ReviewStatus.ABLE);
      orderSubscription.updateStatus(SubscriptionStatus.COMPLETED);

      SubscriptionStatusChangeDto statusChangeDto =
              SubscriptionStatusChangeDto.builder()
                      .orderId(orderSubscription.getOrderSubscriptionId())
                      .subscriptionStatus(orderSubscription.getSubscriptionStatus().toString())
                      .reviewStatus(ReviewStatus.ABLE)
                      .build();

      // order-query로 구독주문 상태 kafka send
      subscriptionStatusChangeDtoKafkaProducer.send("subscription-status-update", statusChangeDto);
    }
  }

  public void processSubscriptionBatch(SubscriptionBatchDtoList subscriptionBatchDtoList ){
    // payment-service로 결제 요청
    feignHandler.processSubscription(subscriptionBatchDtoList);

    List<OrderSubscription> orderSubscriptionList = orderService.readOrderSubscriptionsForBatch(
            subscriptionBatchDtoList);

    for (OrderSubscription orderSubscription : orderSubscriptionList) {
      // SNS로 신규 주문 발생 이벤트 보내기
      NewOrderEvent newOrderEvent =
              OrderCommonMapper.createNewOrderEventListForSubscription(orderSubscription);
      orderSNSPublisher.newOrderEventPublish(newOrderEvent);

      // SQS로 고객에게 신규 주문 알리기
      orderSQSPublisher.publishOrderSuccess(
              orderSubscription.getUserId(), orderSubscription.getPhoneNumber());
    }

    // 정기구독 배송일/결제일 업데이트
    List<SubscriptionDateDto> subscriptionDateDtos =
            orderSubscriptionList.stream()
                    .map(
                            orderSubscription ->
                                    SubscriptionDateDto.builder()
                                            .subscriptionId(orderSubscription.getOrderSubscriptionId())
                                            .nextDeliveryDate(orderSubscription.getDeliveryDay())
                                            .nextPaymentDate(orderSubscription.getPaymentDate().toLocalDate())
                                            .build())
                    .collect(Collectors.toList());
    SubscriptionDateDtoList subscriptionDateDtoList =
            SubscriptionDateDtoList.builder().subscriptionDateDtoList(subscriptionDateDtos).build();
    subscriptionDateDtoListKafkaProducer.send("subscription-date-update", subscriptionDateDtoList);
  }

  public void pickupStatusChange(LocalDateTime now, OrderPickupStatus orderPickupStatus){
    List<OrderPickup> pickups = orderService.readPickupsForStatusChange(now);
    pickups.stream()
            .filter(pickup -> pickup.getOrderPickupStatus().equals(OrderPickupStatus.PENDING))
            .forEach(
                    pickup -> {
                      pickup.completeOrderPickup(orderPickupStatus); // 리뷰, 카드 상태 변경
                      PickupStatusChangeDto status =
                              PickupStatusChangeDto.builder()
                                      .orderId(pickup.getOrderPickupId())
                                      .pickupStatus(pickup.getOrderPickupStatus().toString())
                                      .cardStatus(pickup.getOrderPickupProduct().getCardIsWritten())
                                      .reviewStatus(pickup.getOrderPickupProduct().getReviewIsWritten())
                                      .build();
                      pickupStatusUpdateKafkaProducer.send("pickup-status-update", status);
                    });
  }

  public List<IsProductPriceValid> createPriceCheckDto(List<OrderInfoByStore> orderInfoByStores) {
    List<IsProductPriceValid> list = new ArrayList<>();
    for (OrderInfoByStore orderInfoByStore : orderInfoByStores) {
      for (ProductCreate productCreate : orderInfoByStore.getProducts()) {
        String productId = productCreate.getProductId();
        Long price = productCreate.getPrice();
        IsProductPriceValid dto = IsProductPriceValid.toDto(productId, price);
        list.add(dto);
      }
    }
    return list;
  }

  public List<ValidatePriceDto> createCouponAndDeliveryCheckDto(
      List<OrderInfoByStore> orderInfoByStores) {
    List<ValidatePriceDto> list = new ArrayList<>();
    for (OrderInfoByStore orderInfoByStore : orderInfoByStores) {
      ValidatePriceDto dto = ValidatePriceDto.toDto(orderInfoByStore);
      list.add(dto);
    }
    return list;
  }

  public OrderInfoByStore createOrderInfoByStore(OrderForPickupDto requestDto) {
    List<ProductCreate> products = new ArrayList<>();
    ProductCreate productCreate =
        ProductCreate.builder()
            .productId(requestDto.getProduct().getProductId())
            .productName(requestDto.getProduct().getProductName())
            .quantity(requestDto.getProduct().getQuantity())
            .price((requestDto.getProduct().getPrice()))
            .productThumbnailImage(requestDto.getProduct().getProductThumbnailImage())
            .build();
    products.add(productCreate);

    return OrderInfoByStore.builder()
        .storeId(requestDto.getStoreId())
        .storeName(requestDto.getStoreName())
        .products(products)
        .totalAmount(requestDto.getTotalAmount())
        .deliveryCost(requestDto.getDeliveryCost())
        .couponId(requestDto.getCouponId())
        .couponAmount(requestDto.getCouponAmount())
        .actualAmount(requestDto.getActualAmount())
        .build();
  }

  public OrderInfoByStore createOrderInfoByStoreForSubscription(
          OrderForSubscriptionDto requestDto) {
    ProductCreate productCreate =
            ProductCreate.builder()
                    .productId(requestDto.getProducts().getProductId())
                    .productName(requestDto.getProducts().getProductName())
                    .quantity(requestDto.getProducts().getQuantity())
                    .price((requestDto.getProducts().getPrice()))
                    .productThumbnailImage(requestDto.getProducts().getProductThumbnailImage())
                    .build();
    List<ProductCreate> products = List.of(productCreate);

    return OrderInfoByStore.builder()
            .storeId(requestDto.getStoreId())
            .storeName(requestDto.getStoreName())
            .products(products)
            .totalAmount(requestDto.getTotalAmount())
            .deliveryCost(requestDto.getDeliveryCost())
            .couponId(requestDto.getCouponId())
            .couponAmount(requestDto.getCouponAmount())
            .actualAmount(requestDto.getActualAmount())
            .build();
  }
}
