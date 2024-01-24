package kr.bb.order.service;

import static bloomingblooms.domain.notification.delivery.DeliveryStatus.COMPLETED;
import static bloomingblooms.domain.notification.delivery.DeliveryStatus.PROCESSING;

import bloomingblooms.domain.batch.SubscriptionBatchDto;
import bloomingblooms.domain.batch.SubscriptionBatchDtoList;
import bloomingblooms.domain.delivery.DeliveryAddressInsertDto;
import bloomingblooms.domain.delivery.DeliveryInsertDto;
import bloomingblooms.domain.delivery.UpdateOrderStatusDto;
import bloomingblooms.domain.delivery.UpdateOrderSubscriptionStatusDto;
import bloomingblooms.domain.notification.order.OrderType;
import bloomingblooms.domain.order.*;
import bloomingblooms.domain.payment.KakaopayApproveRequestDto;
import bloomingblooms.domain.pickup.PickupCreateDto;
import bloomingblooms.domain.review.ReviewStatus;
import bloomingblooms.domain.subscription.SubscriptionCreateDto;
import bloomingblooms.domain.subscription.SubscriptionDateDto;
import bloomingblooms.dto.command.CartDeleteCommand;
import bloomingblooms.dto.command.CartDeleteDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.persistence.EntityNotFoundException;
import kr.bb.order.entity.OrderDeliveryProduct;
import kr.bb.order.entity.OrderPickupProduct;
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
import kr.bb.order.mapper.OrderProductMapper;
import kr.bb.order.repository.*;
import kr.bb.order.util.OrderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
  private final RedisTemplate<String, OrderInfo> redisTemplate;
  private final RedisTemplate<String, PickupOrderInfo> redisTemplateForPickup;
  private final RedisTemplate<String, SubscriptionOrderInfo> redisTemplateForSubscription;
  private final OrderDeliveryRepository orderDeliveryRepository;
  private final KafkaProducer<ProcessOrderDto> processOrderDtoKafkaProducer;
  private final KafkaProducer<CartDeleteCommand> cartItemDeleteKafkaProducer;
  private final KafkaProducer<PickupCreateDto> pickupCreateDtoKafkaProducer;
  private final KafkaProducer<SubscriptionCreateDto> subscriptionCreateDtoKafkaProducer;
  private final KafkaProducer<SubscriptionDateDtoList> subscriptionDateDtoListKafkaProducer;
  private final KafkaProducer<PickupStatusChangeDto> pickupStatusUpdateKafkaProducer;
  private final KafkaProducer<SubscriptionStatusChangeDto> subscriptionStatusChangeDtoKafkaProducer;
  private final OrderDeliveryProductRepository orderDeliveryProductRepository;
  private final OrderPickupProductRepository orderPickupProductRepository;
  private final OrderGroupRepository orderGroupRepository;
  private final OrderPickupRepository orderPickupRepository;
  private final OrderSubscriptionRepository orderSubscriptionRepository;
  private final OrderSNSPublisher orderSNSPublisher;
  private final OrderSQSPublisher orderSQSPublisher;
  private final FeignHandler feignHandler;
  private OrderService orderService;

  @Autowired
  public void setOrderService(OrderService orderService) {
    this.orderService = orderService;
  }


  // (바로주문, 장바구니 / 픽업주문 / 구독주문) 타 서비스로 kafka 주문 요청 처리 [쿠폰사용, 재고차감]
  @Transactional
  public void requestOrder(String orderId, String orderType, String pgToken) {
    // redis에서 정보 가져오기 및 TTL 갱신
    if (orderType.equals(OrderType.DELIVERY.toString())) {
      OrderInfo orderInfo = redisTemplate.opsForValue().get(orderId);
      if (orderInfo == null) throw new PaymentExpiredException();

      orderInfo.setPgToken(pgToken);
      redisTemplate.opsForValue().set(orderId, orderInfo);
      redisTemplate.expire(orderId, 15, TimeUnit.MINUTES);

      ProcessOrderDto processOrderDto =
          OrderCommonMapper.toProcessOrderDto(orderId, orderType, orderInfo);
      processOrderDtoKafkaProducer.send("coupon-use", processOrderDto);
    } else if (orderType.equals(OrderType.PICKUP.toString())) {
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
    String orderMethod = processOrderDto.getOrderMethod();
    String orderType = processOrderDto.getOrderType();
    if (orderType.equals(OrderType.DELIVERY.toString())
        || orderMethod.equals(OrderMethod.CART.toString())) {
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

      feignHandler.createDeliveryAddress(deliveryAddressInsertDto);

      // SNS로 신규 주문 발생 이벤트 보내기
      NewOrderEvent newOrderEvent =
          OrderCommonMapper.createNewOrderEventListForDelivery(orderGroup, orderInfo);

      log.info("userId ={}", orderInfo.getUserId());

      orderSNSPublisher.newOrderEventPublish(newOrderEvent);

      // SQS로 고객에게 신규 주문 알리기
      orderSQSPublisher.publishOrderSuccess(orderInfo.getUserId(), orderInfo.getOrdererPhoneNumber());

    } else if (orderType.equals(OrderType.PICKUP.toString())) {
      PickupOrderInfo pickupOrderInfo =
          redisTemplateForPickup.opsForValue().get(processOrderDto.getOrderId());
      if (pickupOrderInfo == null) throw new PaymentExpiredException();
      // 자기자신을 주입받아 호출하여 내부호출 해결
      OrderPickup orderPickup = orderService.processOrderPickup(processOrderDto, pickupOrderInfo);

      // SNS로 신규 주문 발생 이벤트 보내기
      NewOrderEvent newOrderEvent =
          OrderCommonMapper.createNewOrderEventListForPickup(orderPickup, pickupOrderInfo);
      orderSNSPublisher.newOrderEventPublish(newOrderEvent);

      // SQS로 고객에게 신규 주문 알리기
      orderSQSPublisher.publishOrderSuccess(
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

  // (바로주문, 장바구니) 주문 저장하기
  @Transactional
  // 만약 성공하면 다른 곳에서도 추가하기. (데이터 읽기 로직과 쓰기를 별도의 서비스 레이어로 분리하여 트랜잭션을 관리도 가능)
  public OrderGroup processOrderDelivery(ProcessOrderDto processOrderDto, OrderInfo orderInfo) {
    // delivery-service로 delivery 정보 저장 및 deliveryId 알아내기
    List<DeliveryInsertDto> dtoList = OrderCommonMapper.toDeliveryInsertDto(orderInfo);
    List<Long> deliveryIds = feignHandler.createDelivery(dtoList);

    // payment-service 최종 결제 승인 요청
    KakaopayApproveRequestDto approveRequestDto = KakaopayMapper.toDtoFromOrderInfo(orderInfo);
    LocalDateTime paymentDateTime = feignHandler.approve(approveRequestDto);

    OrderGroup orderGroup =
        OrderGroup.builder()
            .orderGroupId(processOrderDto.getOrderId())
            .userId(orderInfo.getUserId())
            .build();
    orderGroupRepository.save(orderGroup);

    // 주문 정보 저장
    for (int i = 0; i < orderInfo.getOrderInfoByStores().size(); i++) {
      // 1. 주문_배송 entity
      String orderDeliveryId = OrderUtil.generateUUID();
      OrderDelivery orderDelivery =
          OrderDelivery.toEntity(
              orderDeliveryId,
              deliveryIds.get(i),
              orderInfo.getOrderInfoByStores().get(i));
      // 연관관계 매핑 : 편의 메서드 적용
      orderDelivery.setOrderGroup(orderGroup);
      orderDeliveryRepository.save(orderDelivery);

      // 2. 주문_상품 entity
      List<OrderDeliveryProduct> orderDeliveryProducts = new ArrayList<>();
      for (ProductCreate productCreate : orderInfo.getOrderInfoByStores().get(i).getProducts()) {
        OrderDeliveryProduct orderDeliveryProduct = OrderProductMapper.toEntity(productCreate);
        // 연관관계 매핑 : 편의 메서드 적용
        orderDeliveryProduct.setOrderDelivery(orderDelivery);
        orderDeliveryProducts.add(orderDeliveryProduct);
      }
      orderDeliveryProductRepository.saveAll(orderDeliveryProducts);
    }

    // 장바구니에서 주문이면 장바구니에서 해당 상품들 비우기 kafka 요청
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

    return orderGroup;
  }

  // (픽업주문) 주문 저장하기
  @Transactional
  public OrderPickup processOrderPickup(
      ProcessOrderDto processOrderDto, PickupOrderInfo pickupOrderInfo) {

    // payment-service 최종 결제 승인 요청
    KakaopayApproveRequestDto approveRequestDto =
        KakaopayMapper.toDtoFromPickupOrderInfo(pickupOrderInfo);
    LocalDateTime paymentDateTime = feignHandler.approve(approveRequestDto);

    LocalDateTime pickupDateTime =
        parseDateTime(pickupOrderInfo.getPickupDate(), pickupOrderInfo.getPickupTime());

    OrderPickup orderPickup =
        OrderPickup.builder()
            .orderPickupId(processOrderDto.getOrderId())
            .userId(pickupOrderInfo.getUserId())
            .storeId(pickupOrderInfo.getStoreId())
            .orderPickupTotalAmount(pickupOrderInfo.getTotalAmount())
            .orderPickupCouponAmount(pickupOrderInfo.getCouponAmount())
            .orderPickupDatetime(pickupDateTime)
            .orderPickupPhoneNumber(pickupOrderInfo.getOrdererPhoneNumber())
            .build();

    OrderPickupProduct orderPickupProduct =
        OrderPickupProduct.builder()
            .productId(pickupOrderInfo.getProduct().getProductId())
            .orderProductPrice(pickupOrderInfo.getProduct().getPrice())
            .orderProductQuantity(pickupOrderInfo.getQuantity())
            .build();

    orderPickupProduct.setOrderPickup(orderPickup);
    orderPickupProductRepository.save(orderPickupProduct);
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

    List<Long> deliveryIds = feignHandler.createDelivery(dtoList);

    // payment-service 최종 결제 승인 요청
    KakaopayApproveRequestDto approveRequestDto =
        KakaopayMapper.toDtoFromSubscriptionOrderInfo(subscriptionOrderInfo, deliveryIds);
    LocalDateTime paymentDateTime = feignHandler.approve(approveRequestDto);

    OrderSubscription orderSubscription =
        OrderSubscription.builder()
            .orderSubscriptionId(processOrderDto.getOrderId())
            .userId(processOrderDto.getUserId())
            .subscriptionProductId(subscriptionOrderInfo.getProduct().getProductId())
            .deliveryId(deliveryIds.get(0))
            .productName(subscriptionOrderInfo.getItemName())
            .productPrice(subscriptionOrderInfo.getProduct().getPrice())
            .deliveryDay(LocalDate.now().plusDays(3))
            .storeId(subscriptionOrderInfo.getStoreId())
            .phoneNumber(subscriptionOrderInfo.getOrdererPhoneNumber())
            .paymentDate(LocalDateTime.now().plusDays(30))
            .build();

    orderSubscriptionRepository.save(orderSubscription);

    // order-query 서비스로 구독 주문 Kafka send
    SubscriptionCreateDto subscriptionCreateDto =
        OrderCommonMapper.toSubscriptionCreateDto(
            subscriptionOrderInfo, paymentDateTime, orderSubscription);
    subscriptionCreateDtoKafkaProducer.send("subscription-create", subscriptionCreateDto);

    return orderSubscription;
  }

  @Transactional
  public void updateOrderDeliveryStatus(UpdateOrderStatusDto statusDto) {
    // 배송 주문인 경우
    OrderDelivery orderDelivery =
            orderDeliveryRepository
                    .findById(statusDto.getOrderDeliveryId())
                    .orElseThrow(EntityNotFoundException::new);
    orderDelivery.updateStatus(statusDto.getDeliveryStatus());

    // 배송상태가 완료가 된 경우
    if (COMPLETED.equals(statusDto.getDeliveryStatus())) {
      orderDelivery
              .getOrderDeliveryProducts()
              .forEach(OrderDeliveryProduct::updateReviewAndCardStatus);
    }

    // 배송상태가 배송중인 경우
    if(PROCESSING.equals(statusDto.getDeliveryStatus())){
      orderSQSPublisher.publishDeliveryNotification(orderDelivery.getOrderGroup().getUserId(), statusDto.getPhoneNumber());
    }

  }

  @Transactional
  public void updateOrderSubscriptionStatus(UpdateOrderSubscriptionStatusDto statusDto ){
    List<OrderSubscription> orderSubscriptions = orderSubscriptionRepository.findAllByDeliveryIds(statusDto.getDeliveryIds());

    // 배송상태를 완료로 변경
    for (OrderSubscription orderSubscription : orderSubscriptions) {
      orderSubscription.updateReviewStatus(ReviewStatus.ABLE);
      orderSubscription.updateStatus(SubscriptionStatus.COMPLETED);

      SubscriptionStatusChangeDto statusChangeDto = SubscriptionStatusChangeDto.builder()
              .orderId(orderSubscription.getOrderSubscriptionId())
              .subscriptionStatus(orderSubscription.getSubscriptionStatus().toString())
              .reviewStatus(ReviewStatus.ABLE)
              .build();

      // order-query로 구독주문 상태 kafka send
      subscriptionStatusChangeDtoKafkaProducer.send("subscription-status-update", statusChangeDto);
    }
  }

  @Transactional
  public void processSubscriptionBatch(SubscriptionBatchDtoList subscriptionBatchDtoList ) {
    // payment-service로 결제 요청
    feignHandler.processSubscription(subscriptionBatchDtoList);

    List<OrderSubscription> orderSubscriptionList =
        orderSubscriptionRepository.findAllById(
                subscriptionBatchDtoList.getSubscriptionBatchDtoList().stream().map(
                        SubscriptionBatchDto::getOrderSubscriptionId).collect(
                        Collectors.toList()));

    for (OrderSubscription orderSubscription : orderSubscriptionList) {
      // SNS로 신규 주문 발생 이벤트 보내기
      NewOrderEvent newOrderEvent =
          OrderCommonMapper.createNewOrderEventListForSubscription(orderSubscription);
      orderSNSPublisher.newOrderEventPublish(newOrderEvent);

      // SQS로 고객에게 신규 주문 알리기
      orderSQSPublisher.publishOrderSuccess(orderSubscription.getUserId(), orderSubscription.getPhoneNumber());
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

  // 픽업 상태 변경
  @Transactional
  public void pickupStatusChange(LocalDateTime date, OrderPickupStatus orderPickupStatus ) {
    List<OrderPickup> pickups = orderPickupRepository.findByOrderPickupDatetimeBetween(date.minusDays(1L), date);
    pickups.stream()
            .filter(pickup -> pickup.getOrderPickupStatus().equals(OrderPickupStatus.PENDING))
            .forEach(pickup -> {
              pickup.completeOrderPickup(orderPickupStatus);  // 리뷰, 카드 상태 변경
              PickupStatusChangeDto status = PickupStatusChangeDto.builder()
                      .orderId(pickup.getOrderPickupId())
                      .pickupStatus(pickup.getOrderPickupStatus().toString())
                      .cardStatus(pickup.getOrderPickupProduct().getCardIsWritten())
                      .reviewStatus(pickup.getOrderPickupProduct().getReviewIsWritten())
                      .build();
              pickupStatusUpdateKafkaProducer.send("pickup-status-update", status);
            });
  }

  private LocalDateTime parseDateTime(String pickupDate, String pickupTime) {
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    LocalDate date = LocalDate.parse(pickupDate, dateFormatter);
    LocalTime time = LocalTime.parse(pickupTime, timeFormatter);

    return LocalDateTime.of(date, time);
  }

}
