package kr.bb.order.service;

import bloomingblooms.domain.delivery.DeliveryAddressInsertDto;
import bloomingblooms.domain.delivery.DeliveryInsertDto;
import bloomingblooms.domain.delivery.UpdateOrderStatusDto;
import bloomingblooms.domain.order.NewOrderEvent.NewOrderEventItem;
import bloomingblooms.domain.order.OrderInfoByStore;
import bloomingblooms.domain.order.ProcessOrderDto;
import bloomingblooms.domain.order.ProductCreate;
import bloomingblooms.domain.order.ValidatePriceDto;
import bloomingblooms.domain.payment.KakaopayApproveRequestDto;
import bloomingblooms.domain.payment.KakaopayReadyRequestDto;
import bloomingblooms.domain.payment.KakaopayReadyResponseDto;
import bloomingblooms.domain.pickup.PickupCreateDto;
import bloomingblooms.domain.product.IsProductPriceValid;
import bloomingblooms.domain.subscription.SubscriptionCreateDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.persistence.EntityNotFoundException;
import kr.bb.order.dto.request.orderForDelivery.OrderForDeliveryRequest;
import kr.bb.order.dto.request.orderForPickup.OrderForPickupDto;
import kr.bb.order.dto.request.orderForSubscription.OrderForSubscriptionDto;
import kr.bb.order.entity.OrderDeliveryProduct;
import kr.bb.order.entity.OrderPickupProduct;
import kr.bb.order.entity.OrderType;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.entity.delivery.OrderDeliveryStatus;
import kr.bb.order.entity.delivery.OrderGroup;
import kr.bb.order.entity.pickup.OrderPickup;
import kr.bb.order.entity.redis.OrderInfo;
import kr.bb.order.entity.redis.PickupOrderInfo;
import kr.bb.order.entity.redis.SubscriptionOrderInfo;
import kr.bb.order.entity.subscription.OrderSubscription;
import kr.bb.order.exception.PaymentExpiredException;
import kr.bb.order.feign.DeliveryServiceClient;
import kr.bb.order.feign.PaymentServiceClient;
import kr.bb.order.feign.ProductServiceClient;
import kr.bb.order.feign.StoreServiceClient;
import kr.bb.order.infra.OrderSNSPublisher;
import kr.bb.order.infra.OrderSQSPublisher;
import kr.bb.order.kafka.KafkaProducer;
import kr.bb.order.mapper.DeliveryAddressMapper;
import kr.bb.order.mapper.KakaopayMapper;
import kr.bb.order.mapper.OrderCommonMapper;
import kr.bb.order.mapper.OrderProductMapper;
import kr.bb.order.repository.OrderDeliveryRepository;
import kr.bb.order.repository.OrderGroupRepository;
import kr.bb.order.repository.OrderPickupRepository;
import kr.bb.order.repository.OrderProductRepository;
import kr.bb.order.repository.OrderSubscriptionRepository;
import kr.bb.order.util.OrderUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
  private final RedisTemplate<String, PickupOrderInfo> redisTemplateForPickup;
  private final RedisTemplate<String, SubscriptionOrderInfo> redisTemplateForSubscription;
  private final OrderManager orderManager;
  private final OrderDeliveryRepository orderDeliveryRepository;
  private final DeliveryServiceClient deliveryServiceClient;
  private final KafkaProducer<ProcessOrderDto> processOrderDtoKafkaProducer;
  private final KafkaProducer<Map<Long, String>> cartItemDeleteKafkaProducer;
  private final KafkaProducer<PickupCreateDto> pickupCreateDtoKafkaProducer;
  private final KafkaProducer<SubscriptionCreateDto> subscriptionCreateDtoKafkaProducer;
  private final OrderUtil orderUtil;
  private final OrderProductRepository orderProductRepository;
  private final OrderGroupRepository orderGroupRepository;
  private final OrderPickupRepository orderPickupRepository;
  private final OrderSubscriptionRepository orderSubscriptionRepository;
  private final OrderSNSPublisher orderSNSPublisher;
  private final OrderSQSPublisher orderSQSPublisher;
  private OrderService orderService;

  @Autowired
  public void setOrderService(OrderService orderService) {
    this.orderService = orderService;
  }

  // 바로 주문 / 장바구니 주문 준비 단계
  @Transactional
  public KakaopayReadyResponseDto readyForOrder(
      Long userId, OrderForDeliveryRequest requestDto, OrderType orderType) {
    // 결제금액, 재고유무, 쿠폰유효유무 feign을 통해 확인하기

    // product-service로 가격 유효성 확인하기
    List<IsProductPriceValid> priceCheckDtos =
        createPriceCheckDto(requestDto.getOrderInfoByStores());
    productServiceClient.validatePrice(priceCheckDtos);

    // store-service로 쿠폰(가격, 상태), 배송비 정책 확인하기
    List<ValidatePriceDto> couponAndDeliveryCheckDtos =
        createCouponAndDeliveryCheckDto(requestDto.getOrderInfoByStores());
    storeServiceClient.validatePurchaseDetails(couponAndDeliveryCheckDtos);

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
            orderType.toString(),
            requestDto.getOrderInfoByStores(),
            requestDto.getSumOfActualAmount(),
            isSubscriptionPay);

    // payment-service로 결제 준비 요청
    KakaopayReadyResponseDto responseDto = paymentServiceClient.ready(readyRequestDto).getData();

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
            orderType);

    redisTemplate.opsForValue().set(tempOrderId, orderInfo);

    return responseDto;
  }

  // 픽업 주문 준비 단계
  @Transactional
  public KakaopayReadyResponseDto readyForPickupOrder(
      Long userId, OrderForPickupDto requestDto, OrderType orderType) {

    List<OrderInfoByStore> orderInfoByStores = new ArrayList<>();
    OrderInfoByStore orderInfoByStore = createOrderInfoByStore(requestDto);
    orderInfoByStores.add(orderInfoByStore);

    // product-service로 가격 유효성 확인하기
    List<IsProductPriceValid> priceCheckDtos = createPriceCheckDto(orderInfoByStores);
    productServiceClient.validatePrice(priceCheckDtos);

    // store-service로 쿠폰(가격, 상태), 배송비 정책 확인하기
    List<ValidatePriceDto> validatePriceDtos = createCouponAndDeliveryCheckDto(orderInfoByStores);
    storeServiceClient.validatePurchaseDetails(validatePriceDtos);

    // 유효성 검사를 다 통과했다면 이젠 OrderManager를 통해 총 결제 금액이 맞는지 확인하기
    orderManager.checkActualAmountIsValid(orderInfoByStores, requestDto.getActualAmount());

    // 임시 주문id 및 결제준비용 dto 생성
    String tempOrderId = orderUtil.generateUUID();
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
    KakaopayReadyResponseDto responseDto = paymentServiceClient.ready(readyRequestDto).getData();

    // 주문정보와 tid를 redis에 저장
    String itemName = readyRequestDto.getItemName();
    long quantity = readyRequestDto.getQuantity();
    String tid = responseDto.getTid();
    PickupOrderInfo pickupOrderInfo =
        PickupOrderInfo.convertToRedisDto(
            tempOrderId, userId, itemName, quantity, isSubscriptionPay, tid, requestDto, orderType);

    redisTemplateForPickup.opsForValue().set(tempOrderId, pickupOrderInfo);

    return responseDto;
  }

  // 구독 주문 준비 단계
  @Transactional
  public KakaopayReadyResponseDto readyForSubscriptionOrder(
      Long userId, OrderForSubscriptionDto requestDto, OrderType orderType) {
    OrderInfoByStore orderInfoByStore = createOrderInfoByStoreForSubscription(requestDto);
    List<OrderInfoByStore> orderInfoByStores = List.of(orderInfoByStore);

    // product-service로 가격 유효성 확인하기
    List<IsProductPriceValid> priceCheckDtos = createPriceCheckDto(orderInfoByStores);
    productServiceClient.validatePrice(priceCheckDtos);

    // store-service로 쿠폰(가격, 상태), 배송비 정책 확인하기
    List<ValidatePriceDto> validatePriceDtos = createCouponAndDeliveryCheckDto(orderInfoByStores);
    storeServiceClient.validatePurchaseDetails(validatePriceDtos);

    // 유효성 검사를 다 통과했다면 이젠 OrderManager를 통해 총 결제 금액이 맞는지 확인하기
    orderManager.checkActualAmountIsValid(orderInfoByStores, requestDto.getActualAmount());

    // 임시 주문id 및 결제준비용 dto 생성
    String tempOrderId = orderUtil.generateUUID();
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
    KakaopayReadyResponseDto responseDto = paymentServiceClient.ready(readyRequestDto).getData();

    // 주문정보와 tid를 redis에 저장
    String itemName = readyRequestDto.getItemName();
    long quantity = readyRequestDto.getQuantity();
    String tid = responseDto.getTid();
    SubscriptionOrderInfo subscriptionOrderInfo =
        SubscriptionOrderInfo.convertToRedisDto(
            tempOrderId, userId, itemName, quantity, isSubscriptionPay, tid, requestDto, orderType);

    redisTemplateForSubscription.opsForValue().set(tempOrderId, subscriptionOrderInfo);

    return responseDto;
  }

  // (바로주문, 장바구니 / 픽업주문 / 구독주문) 타 서비스로 kafka 주문 요청 처리 [쿠폰사용, 재고차감]
  @Transactional
  public void requestOrder(String orderId, String orderType, String pgToken) {
    // redis에서 정보 가져오기 및 TTL 갱신
    if (orderType.equals(OrderType.ORDER_DELIVERY.toString())) {
      OrderInfo orderInfo = redisTemplate.opsForValue().get(orderId);
      if (orderInfo == null) throw new PaymentExpiredException();

      orderInfo.setPgToken(pgToken);
      redisTemplate.opsForValue().set(orderId, orderInfo);
      redisTemplate.expire(orderId, 5, TimeUnit.MINUTES);

      ProcessOrderDto processOrderDto =
          OrderCommonMapper.toProcessOrderDto(orderId, orderType, orderInfo);
      processOrderDtoKafkaProducer.send("coupon-use", processOrderDto);
    } else if (orderType.equals(OrderType.ORDER_PICKUP.toString())) {
      PickupOrderInfo pickupOrderInfo = redisTemplateForPickup.opsForValue().get(orderId);
      if (pickupOrderInfo == null) throw new PaymentExpiredException();

      pickupOrderInfo.setPgToken(pgToken);
      redisTemplateForPickup.opsForValue().set(orderId, pickupOrderInfo);
      redisTemplateForPickup.expire(orderId, 5, TimeUnit.MINUTES);

      ProcessOrderDto processOrderDto =
          OrderCommonMapper.toDtoForOrderPickup(orderId, pickupOrderInfo);
      processOrderDtoKafkaProducer.send("coupon-use", processOrderDto);
    } else {
      SubscriptionOrderInfo subscriptionOrderInfo =
          redisTemplateForSubscription.opsForValue().get(orderId);
      if (subscriptionOrderInfo == null) throw new PaymentExpiredException();

      subscriptionOrderInfo.setPgToken(pgToken);
      redisTemplateForSubscription.opsForValue().set(orderId, subscriptionOrderInfo);
      redisTemplateForSubscription.expire(orderId, 5, TimeUnit.MINUTES);

      ProcessOrderDto processOrderDto =
          OrderCommonMapper.toDtoForOrderSubscription(orderId, subscriptionOrderInfo);
      processOrderDtoKafkaProducer.send("coupon-use", processOrderDto);
    }
  }

  // 주문 저장하기
  public void processOrder(ProcessOrderDto processOrderDto) {
    String orderType = processOrderDto.getOrderType();
    if (orderType.equals(OrderType.ORDER_DELIVERY.toString())
        || orderType.equals(OrderType.ORDER_CART.toString())) {
      OrderInfo orderInfo = redisTemplate.opsForValue().get(processOrderDto.getOrderId());
      if (orderInfo == null) throw new PaymentExpiredException();
      // 자기자신을 주입받아 호출하여 내부호출 해결
      OrderGroup orderGroup = orderService.processOrderDelivery(processOrderDto, orderInfo);

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

      deliveryServiceClient.createDeliveryAddress(deliveryAddressInsertDto);

      // SNS로 신규 주문 발생 이벤트 보내기
      List<NewOrderEventItem> newOrderEventList =
          OrderCommonMapper.createNewOrderEventListForDelivery(orderGroup, orderInfo);
      orderSNSPublisher.newOrderEventPublish(newOrderEventList);

      // SQS로 고객에게 신규 주문 알리기
      orderSQSPublisher.publish(orderInfo.getUserId(), orderInfo.getOrdererPhoneNumber());

    } else if (orderType.equals(OrderType.ORDER_PICKUP.toString())) {
      PickupOrderInfo pickupOrderInfo =
          redisTemplateForPickup.opsForValue().get(processOrderDto.getOrderId());
      if (pickupOrderInfo == null) throw new PaymentExpiredException();
      // 자기자신을 주입받아 호출하여 내부호출 해결
      OrderPickup orderPickup = orderService.processOrderPickup(processOrderDto, pickupOrderInfo);

      // SNS로 신규 주문 발생 이벤트 보내기
      List<NewOrderEventItem> newOrderEventList =
          OrderCommonMapper.createNewOrderEventListForPickup(orderPickup, pickupOrderInfo);
      orderSNSPublisher.newOrderEventPublish(newOrderEventList);

      // SQS로 고객에게 신규 주문 알리기
      orderSQSPublisher.publish(
          pickupOrderInfo.getUserId(), pickupOrderInfo.getOrdererPhoneNumber());
    } else {
      SubscriptionOrderInfo subscriptionOrderInfo =
          redisTemplateForSubscription.opsForValue().get(processOrderDto.getOrderId());
      if (subscriptionOrderInfo == null) throw new PaymentExpiredException();

      // 자기자신을 주입받아 호출하여 내부호출 해결
      OrderSubscription orderSubscription =
          orderService.processOrderSubscription(processOrderDto, subscriptionOrderInfo);

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

      deliveryServiceClient.createDeliveryAddress(deliveryAddressInsertDto);

      // SNS로 신규 주문 발생 이벤트 보내기
      List<NewOrderEventItem> newOrderEventList =
          OrderCommonMapper.createNewOrderEventListForSubscription(
              orderSubscription, subscriptionOrderInfo);
      orderSNSPublisher.newOrderEventPublish(newOrderEventList);

      // SQS로 고객에게 신규 주문 알리기
      orderSQSPublisher.publish(
          subscriptionOrderInfo.getUserId(), subscriptionOrderInfo.getOrdererPhoneNumber());
    }
  }

  // (바로주문, 장바구니) 주문 저장하기
  @Transactional
  public OrderGroup processOrderDelivery(ProcessOrderDto processOrderDto, OrderInfo orderInfo) {
    // delivery-service로 delivery 정보 저장 및 deliveryId 알아내기
    List<DeliveryInsertDto> dtoList = OrderCommonMapper.toDeliveryInsertDto(orderInfo);
    List<Long> deliveryIds = deliveryServiceClient.createDelivery(dtoList).getData();

    // payment-service 최종 결제 승인 요청
    KakaopayApproveRequestDto approveRequestDto = KakaopayMapper.toDtoFromOrderInfo(orderInfo);
    paymentServiceClient.approve(approveRequestDto).getData();

    OrderGroup orderGroup =
        OrderGroup.builder()
            .orderGroupId(processOrderDto.getOrderId())
            .userId(orderInfo.getUserId())
            .build();
    orderGroupRepository.save(orderGroup);

    // 주문 정보 저장
    for (int i = 0; i < deliveryIds.size(); i++) {
      // 1. 주문_배송 entity
      String orderDeliveryId = orderUtil.generateUUID();
      OrderDelivery orderDelivery =
          OrderDelivery.toEntity(
              orderDeliveryId,
              deliveryIds.get(i),
              orderGroup,
              orderInfo.getOrderInfoByStores().get(i));
      // 연관관계 매핑 : 편의 메서드 적용
      orderDelivery.setOrderGroup(orderGroup);
      orderDeliveryRepository.save(orderDelivery);

      // 2. 주문_상품 entity
      List<OrderDeliveryProduct> orderDeliveryProducts = new ArrayList<>();
      for (OrderInfoByStore orderInfoByStore : orderInfo.getOrderInfoByStores()) {
        for (ProductCreate productCreate : orderInfoByStore.getProducts()) {
          OrderDeliveryProduct orderDeliveryProduct = OrderProductMapper.toEntity(productCreate);
          // 연관관계 매핑 : 편의 메서드 적용
          orderDeliveryProduct.setOrderDelivery(orderDelivery);
          orderDeliveryProducts.add(orderDeliveryProduct);
        }
      }
      orderProductRepository.saveAll(orderDeliveryProducts);
    }

    // 장바구니에서 주문이면 장바구니에서 해당 상품들 비우기 kafka 요청
    if (orderInfo.getOrderType().equals(OrderType.ORDER_CART.toString())) {
      List<String> productIds =
          orderInfo.getOrderInfoByStores().stream()
              .flatMap(orderInfoByStore -> orderInfoByStore.getProducts().stream())
              .map(ProductCreate::getProductId)
              .collect(Collectors.toList());
      Map<Long, String> userIdToProductIdMap = new HashMap<>();
      for (String productId : productIds) {
        userIdToProductIdMap.put(orderInfo.getUserId(), productId);
      }
      cartItemDeleteKafkaProducer.send("delete-from-cart", userIdToProductIdMap);
    }

    return orderGroup;
  }

  // (픽업주문) 주문 저장하기
  @Transactional
  public OrderPickup processOrderPickup(
      ProcessOrderDto processOrderDto, PickupOrderInfo pickupOrderInfo) {

    // payment-service 최종 결제 승인 요청
    KakaopayApproveRequestDto approveRequestDto =
        KakaopayMapper.toDtoFromPickupOrderInfo(pickupOrderInfo);
    LocalDateTime paymentDateTime = paymentServiceClient.approve(approveRequestDto).getData();

    LocalDateTime pickupDateTime =
        parseDateTime(pickupOrderInfo.getPickupDate(), pickupOrderInfo.getPickupTime());

    OrderPickup orderPickup =
        OrderPickup.builder()
            .orderPickupId(processOrderDto.getOrderId())
            .userId(pickupOrderInfo.getUserId())
            .orderPickupTotalAmount(pickupOrderInfo.getTotalAmount())
            .orderPickupCouponAmount(pickupOrderInfo.getCouponAmount())
            .orderPickupDatetime(pickupDateTime)
            .build();

    OrderPickupProduct orderPickupProduct =
        OrderPickupProduct.builder()
            .productId(pickupOrderInfo.getProduct().getProductId())
            .orderProductPrice(pickupOrderInfo.getProduct().getPrice())
            .orderProductQuantity(pickupOrderInfo.getQuantity())
            .build();

    orderPickupProduct.setOrderPickup(orderPickup);
    orderPickupRepository.save(orderPickup);

    // order-query 서비스로 픽업 주문 Kafka send
    PickupCreateDto pickupCreateDto =
        OrderCommonMapper.toPickupCreateDto(pickupOrderInfo, paymentDateTime, orderPickupProduct);
    pickupCreateDtoKafkaProducer.send("pickup-create", pickupCreateDto);

    return orderPickup;
  }

  // (구독주문) 주문 저장하기
  @Transactional
  public OrderSubscription processOrderSubscription(
      ProcessOrderDto processOrderDto, SubscriptionOrderInfo subscriptionOrderInfo) {
    // delivery-service로 delivery 정보 저장 및 deliveryId 알아내기
    List<DeliveryInsertDto> dtoList =
        OrderCommonMapper.toDeliveryInsertDtoForSubscription(subscriptionOrderInfo);
    List<Long> deliveryIds = deliveryServiceClient.createDelivery(dtoList).getData();

    // payment-service 최종 결제 승인 요청
    KakaopayApproveRequestDto approveRequestDto =
        KakaopayMapper.toDtoFromSubscriptionOrderInfo(subscriptionOrderInfo, deliveryIds);
    LocalDateTime paymentDateTime = paymentServiceClient.approve(approveRequestDto).getData();

    OrderSubscription orderSubscription =
        OrderSubscription.builder()
            .orderSubscriptionId(processOrderDto.getOrderId())
            .userId(processOrderDto.getUserId())
            .subscriptionProductId(subscriptionOrderInfo.getProduct().getProductId())
            .deliveryId(deliveryIds.get(0))
            .productName(subscriptionOrderInfo.getItemName())
            .productPrice(subscriptionOrderInfo.getProduct().getPrice())
            .deliveryDay(LocalDate.now().plusDays(3))
            .build();

    orderSubscriptionRepository.save(orderSubscription);

    // order-query 서비스로 구독 주문 Kafka send
    SubscriptionCreateDto subscriptionCreateDto =
        OrderCommonMapper.toSubscriptionCreateDto(
            subscriptionOrderInfo, paymentDateTime, orderSubscription);
//    subscriptionCreateDtoKafkaProducer.send("subscription-create", subscriptionCreateDto);

    return orderSubscription;
  }

  public void updateStatus(UpdateOrderStatusDto statusDto) {
    OrderDelivery orderDelivery =
        orderDeliveryRepository
            .findById(statusDto.getOrderDeliveryId())
            .orElseThrow(EntityNotFoundException::new);
    orderDelivery.updateStatus(OrderDeliveryStatus.valueOf(statusDto.getDeliveryStatus().name()));
    if ("COMPLETED".equalsIgnoreCase(String.valueOf(statusDto.getDeliveryStatus()))) {
      orderDelivery
          .getOrderDeliveryProducts()
          .forEach(OrderDeliveryProduct::updateReviewAndCardStatus);
    }
  }

  private LocalDateTime parseDateTime(String pickupDate, String pickupTime) {
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    LocalDate date = LocalDate.parse(pickupDate, dateFormatter);
    LocalTime time = LocalTime.parse(pickupTime, timeFormatter);

    return LocalDateTime.of(date, time);
  }

  public List<IsProductPriceValid> createPriceCheckDto(List<OrderInfoByStore> orderInfoByStores) {
    List<IsProductPriceValid> list = new ArrayList<>();
    for (bloomingblooms.domain.order.OrderInfoByStore orderInfoByStore : orderInfoByStores) {
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
