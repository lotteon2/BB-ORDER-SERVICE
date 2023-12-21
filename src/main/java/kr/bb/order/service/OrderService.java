package kr.bb.order.service;

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
import kr.bb.order.dto.request.delivery.DeliveryInsertRequestDto;
import kr.bb.order.dto.request.orderForDelivery.OrderForDeliveryRequest;
import kr.bb.order.dto.request.orderForDelivery.OrderInfoByStore;
import kr.bb.order.dto.request.orderForDelivery.ProductCreate;
import kr.bb.order.dto.request.orderForPickup.OrderForPickupDto;
import kr.bb.order.dto.request.payment.KakaopayApproveRequestDto;
import kr.bb.order.dto.request.payment.KakaopayReadyRequestDto;
import kr.bb.order.dto.request.product.PriceCheckDto;
import kr.bb.order.dto.request.store.CouponAndDeliveryCheckDto;
import kr.bb.order.dto.response.payment.KakaopayReadyResponseDto;
import kr.bb.order.entity.OrderDeliveryProduct;
import kr.bb.order.entity.OrderPickupProduct;
import kr.bb.order.entity.OrderType;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.entity.delivery.OrderDeliveryStatus;
import kr.bb.order.entity.delivery.OrderGroup;
import kr.bb.order.entity.pickup.OrderPickup;
import kr.bb.order.entity.redis.OrderInfo;
import kr.bb.order.entity.redis.PickupOrderInfo;
import kr.bb.order.exception.PaymentExpiredException;
import kr.bb.order.feign.DeliveryServiceClient;
import kr.bb.order.feign.PaymentServiceClient;
import kr.bb.order.feign.ProductServiceClient;
import kr.bb.order.feign.StoreServiceClient;
import kr.bb.order.kafka.KafkaProducer;
import kr.bb.order.kafka.ProcessOrderDto;
import kr.bb.order.kafka.UpdateOrderStatusDto;
import kr.bb.order.repository.OrderDeliveryRepository;
import kr.bb.order.repository.OrderGroupRepository;
import kr.bb.order.repository.OrderPickupRepository;
import kr.bb.order.repository.OrderProductRepository;
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
  private final OrderManager orderManager;
  private final OrderDeliveryRepository orderDeliveryRepository;
  private final DeliveryServiceClient deliveryServiceClient;
  private final KafkaProducer kafkaProducer;
  private final OrderUtil orderUtil;
  private final OrderProductRepository orderProductRepository;
  private final OrderGroupRepository orderGroupRepository;
  private final OrderPickupRepository orderPickupRepository;
  private OrderService orderService;

  @Autowired
  public void setOrderService(OrderService orderService ){
    this.orderService = orderService;
  }

  // 바로 주문 / 장바구니 주문 준비 단계
  @Transactional
  public KakaopayReadyResponseDto readyForOrder(
      Long userId, OrderForDeliveryRequest requestDto, OrderType orderType) {
    // 결제금액, 재고유무, 쿠폰유효유무 feign을 통해 확인하기

    // product-service로 가격 유효성 확인하기
    List<PriceCheckDto> priceCheckDtos = createPriceCheckDto(requestDto.getOrderInfoByStores());
        productServiceClient.validatePrice(priceCheckDtos);

    // store-service로 쿠폰(가격, 상태), 배송비 정책 확인하기
    List<CouponAndDeliveryCheckDto> couponAndDeliveryCheckDtos =
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
        OrderInfo.transformDataForApi(
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
    List<PriceCheckDto> priceCheckDtos = createPriceCheckDto(orderInfoByStores);
    productServiceClient.validatePrice(priceCheckDtos);

    // store-service로 쿠폰(가격, 상태), 배송비 정책 확인하기
    List<CouponAndDeliveryCheckDto> couponAndDeliveryCheckDtos =
        createCouponAndDeliveryCheckDto(orderInfoByStores);
    storeServiceClient.validatePurchaseDetails(couponAndDeliveryCheckDtos);

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
        PickupOrderInfo.transformDataForApi(
            tempOrderId, userId, itemName, quantity, isSubscriptionPay, tid, requestDto, orderType);

    redisTemplateForPickup.opsForValue().set(tempOrderId, pickupOrderInfo);

    return responseDto;
  }

  // (바로주문, 장바구니 / 픽업주문) 타 서비스로 kafka 주문 요청 처리
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
          ProcessOrderDto.toDtoForOrderDelivery(orderId, orderType, orderInfo);
      kafkaProducer.requestOrder(processOrderDto);
    } else if (orderType.equals(OrderType.ORDER_PICKUP.toString())) {
      PickupOrderInfo pickupOrderInfo = redisTemplateForPickup.opsForValue().get(orderId);
      if (pickupOrderInfo == null) throw new PaymentExpiredException();

      pickupOrderInfo.setPgToken(pgToken);
      redisTemplateForPickup.opsForValue().set(orderId, pickupOrderInfo);
      redisTemplateForPickup.expire(orderId, 5, TimeUnit.MINUTES);

      ProcessOrderDto processOrderDto =
          ProcessOrderDto.toDtoForOrderPickup(orderId, pickupOrderInfo);
      kafkaProducer.requestOrder((processOrderDto));
    }
  }

  //  주문 저장하기
  public void processOrder(ProcessOrderDto processOrderDto) {
    String orderType = processOrderDto.getOrderType();
    if (orderType.equals(OrderType.ORDER_DELIVERY.toString()) || orderType.equals(OrderType.ORDER_CART.toString())) {
      OrderInfo orderInfo = redisTemplate.opsForValue().get(processOrderDto.getOrderId());
      if (orderInfo == null) throw new PaymentExpiredException();
      // 자기자신을 주입받아 호출하여 내부호출 해결
      orderService.processOrderDelivery(processOrderDto, orderInfo);
    } else if (orderType.equals(OrderType.ORDER_PICKUP.toString())) {
      PickupOrderInfo pickupOrderInfo =
          redisTemplateForPickup.opsForValue().get(processOrderDto.getOrderId());
      if (pickupOrderInfo == null) throw new PaymentExpiredException();
      // 자기자신을 주입받아 호출하여 내부호출 해결
      orderService.processOrderPickup(processOrderDto, pickupOrderInfo);
    }
  }

  // (바로주문, 장바구니) 주문 저장하기
  @Transactional
  public void processOrderDelivery(ProcessOrderDto processOrderDto, OrderInfo orderInfo) {
    // delivery-service로 delivery 정보 저장 및 deliveryId 알아내기
    List<DeliveryInsertRequestDto> dtoList = DeliveryInsertRequestDto.toDto(orderInfo);
    List<Long> deliveryIds = deliveryServiceClient.createDelivery(dtoList).getData();

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
          OrderDeliveryProduct orderDeliveryProduct = ProductCreate.toEntity(productCreate);
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
      Map<Long, String> productIdMap = new HashMap<>();
      for (String productId : productIds) {
        productIdMap.put(orderInfo.getUserId(), productId);
      }
      kafkaProducer.deleteFromCart(productIdMap);
    }

    // payment-service 결제 승인 요청
    KakaopayApproveRequestDto approveRequestDto =
        KakaopayApproveRequestDto.toDto(orderInfo, orderInfo.getOrderType());
    paymentServiceClient.approve(approveRequestDto).getData();
  }

  // (픽업주문) 주문 저장하기
  @Transactional
  public void processOrderPickup(ProcessOrderDto processOrderDto, PickupOrderInfo pickupOrderInfo) {
    LocalDateTime localDateTime =
        parseDateTime(pickupOrderInfo.getPickupDate(), pickupOrderInfo.getPickupTime());

    OrderPickup orderPickup =
        OrderPickup.builder()
            .orderPickupId(processOrderDto.getOrderId())
            .userId(pickupOrderInfo.getUserId())
            .orderPickupTotalAmount(pickupOrderInfo.getTotalAmount())
            .orderPickupCouponAmount(pickupOrderInfo.getCouponAmount())
            .orderPickupDatetime(localDateTime)
            .build();

    OrderPickupProduct orderPickupProduct =
        OrderPickupProduct.builder()
            .productId(pickupOrderInfo.getProduct().getProductId())
            .orderProductPrice(pickupOrderInfo.getProduct().getPrice())
            .orderProductQuantity(pickupOrderInfo.getQuantity())
            .build();

    orderPickupProduct.setOrderPickup(orderPickup);
    orderPickupRepository.save(orderPickup);
  }

  public void updateStatus(UpdateOrderStatusDto statusDto) {
    OrderDelivery orderDelivery =
        orderDeliveryRepository
            .findById(statusDto.getOrderDeliveryId())
            .orElseThrow(EntityNotFoundException::new);
    orderDelivery.updateStatus(statusDto.getStatus());
    if (statusDto.getStatus().equals(OrderDeliveryStatus.COMPLETED.toString())) {
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

  public List<PriceCheckDto> createPriceCheckDto(List<OrderInfoByStore> orderInfoByStores) {
    List<PriceCheckDto> list = new ArrayList<>();
    for (OrderInfoByStore orderInfoByStore : orderInfoByStores) {
      for (ProductCreate productCreate : orderInfoByStore.getProducts()) {
        String productId = productCreate.getProductId();
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
}
