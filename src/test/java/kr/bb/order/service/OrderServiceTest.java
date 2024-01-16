package kr.bb.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import bloomingblooms.domain.batch.SubscriptionBatchDto;
import bloomingblooms.domain.batch.SubscriptionBatchDtoList;
import bloomingblooms.domain.delivery.UpdateOrderStatusDto;
import bloomingblooms.domain.delivery.UpdateOrderSubscriptionStatusDto;
import bloomingblooms.domain.notification.delivery.DeliveryStatus;
import bloomingblooms.domain.notification.order.OrderType;
import bloomingblooms.domain.order.OrderInfoByStore;
import bloomingblooms.domain.order.OrderMethod;
import bloomingblooms.domain.order.PickupStatusChangeDto;
import bloomingblooms.domain.order.ProcessOrderDto;
import bloomingblooms.domain.order.ProductCreate;
import bloomingblooms.domain.order.SubscriptionStatusChangeDto;
import bloomingblooms.domain.payment.KakaopayReadyResponseDto;
import bloomingblooms.domain.pickup.PickupCreateDto;
import bloomingblooms.domain.subscription.SubscriptionCreateDto;
import bloomingblooms.dto.command.CartDeleteCommand;
import bloomingblooms.response.CommonResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import kr.bb.order.dto.request.orderForDelivery.OrderForDeliveryRequest;
import kr.bb.order.dto.request.orderForPickup.OrderForPickupDto;
import kr.bb.order.dto.request.orderForSubscription.OrderForSubscriptionDto;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.entity.pickup.OrderPickup;
import kr.bb.order.entity.pickup.OrderPickupStatus;
import kr.bb.order.entity.redis.OrderInfo;
import kr.bb.order.entity.redis.PickupOrderInfo;
import kr.bb.order.entity.redis.SubscriptionOrderInfo;
import kr.bb.order.entity.subscription.OrderSubscription;
import kr.bb.order.exception.InvalidOrderAmountException;
import kr.bb.order.feign.*;
import kr.bb.order.infra.OrderSNSPublisher;
import kr.bb.order.infra.OrderSQSPublisher;
import kr.bb.order.kafka.KafkaConsumer;
import kr.bb.order.kafka.KafkaProducer;
import kr.bb.order.kafka.SubscriptionDateDtoList;
import kr.bb.order.mapper.OrderCommonMapper;
import kr.bb.order.repository.*;
import kr.bb.order.util.OrderUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;

@SpringBootTest
@Testcontainers
@Transactional
class OrderServiceTest extends AbstractContainerBaseTest {
  @MockBean private ProductServiceClient productServiceClient;
  @Autowired private OrderService orderService;
  @MockBean private StoreServiceClient storeServiceClient;
  @MockBean private PaymentServiceClient paymentServiceClient;
  @Autowired private RedisTemplate<String, OrderInfo> redisTemplate;
  @Autowired private RedisTemplate<String, PickupOrderInfo> redisTemplateForPickup;
  @Autowired private RedisTemplate<String, SubscriptionOrderInfo> redisTemplateForSubscription;
  @Autowired private OrderManager orderManager;
  @Autowired private OrderDeliveryRepository orderDeliveryRepository;
  @MockBean private DeliveryServiceClient deliveryServiceClient;
  @MockBean private KafkaProducer<ProcessOrderDto> processOrderDtoKafkaProducer;
  @MockBean private KafkaProducer<CartDeleteCommand> cartItemDeleteProducer;
  @MockBean private KafkaProducer<PickupCreateDto> pickupCreateDtoKafkaProducer;
  @MockBean private KafkaProducer<SubscriptionCreateDto> subscriptionCreateDtoKafkaProducer;
  @Autowired private KafkaConsumer<ProcessOrderDto> kafkaConsumer;
  @MockBean private KafkaProducer<SubscriptionDateDtoList> subscriptionDateDtoListKafkaProducer;
  @MockBean private KafkaProducer<SubscriptionStatusChangeDto> subscriptionStatusChangeDtoKafkaProducer;
  @MockBean private OrderUtil orderUtil;
  @Autowired private OrderDeliveryProductRepository orderProductRepository;
  @Autowired private OrderPickupProductRepository orderPickupProductRepository;
  @Autowired private OrderGroupRepository orderGroupRepository;
  @Autowired private OrderPickupRepository orderPickupRepository;
  @Autowired private OrderSubscriptionRepository orderSubscriptionRepository;
  @MockBean private OrderSNSPublisher orderSNSPublisher;
  @MockBean private OrderSQSPublisher orderSQSPublisher;
  @MockBean private FeignHandler feignHandler;
  @MockBean private SimpleMessageListenerContainer simpleMessageListenerContainer;
  @MockBean private KafkaProducer<PickupStatusChangeDto> pickupStatusUpdateKafkaProducer;
  @Autowired private EntityManager em;
  @BeforeEach
  void setup() {
    orderService =
        new OrderService(
            redisTemplate,
            redisTemplateForPickup,
            redisTemplateForSubscription,
            orderManager,
            orderDeliveryRepository,
            deliveryServiceClient,
            processOrderDtoKafkaProducer,
            cartItemDeleteProducer,
            pickupCreateDtoKafkaProducer,
            subscriptionCreateDtoKafkaProducer,
            subscriptionDateDtoListKafkaProducer,
            pickupStatusUpdateKafkaProducer,
            subscriptionStatusChangeDtoKafkaProducer,
            orderUtil,
            orderProductRepository,
            orderPickupProductRepository,
            orderGroupRepository,
            orderPickupRepository,
            orderSubscriptionRepository,
            orderSNSPublisher,
            orderSQSPublisher,
            feignHandler
            );
  }

  @AfterEach
  void teardown() {
    redisTemplate.getConnectionFactory().getConnection().flushDb();
  }

  @Test
  @DisplayName("바로 주문하기 - 준비단계")
  public void readyForDeliveryOrder() {
    Long userId = 1L;
    String orderId = "임시orderId";
    Long sumOfActualAmount = 90500L;
    OrderForDeliveryRequest request = createOrderForDeliveryRequest(sumOfActualAmount);

    doNothing().when(feignHandler).validatePrice(any());
    doNothing().when(feignHandler).validatePurchaseDetails(any());
    KakaopayReadyResponseDto mockResponseDto = createKakaopayReadyResponseDto();
    when(feignHandler.ready(any())).thenReturn(mockResponseDto);
    when(orderUtil.generateUUID()).thenReturn(orderId);

    KakaopayReadyResponseDto responseDto =
        orderService.readyForOrder(userId, request, OrderType.DELIVERY, OrderMethod.DIRECT);

    assertNotNull(responseDto);
  }

  @Test
  @DisplayName("잘못된 결제금액으로는 주문이 불가능하다")
  public void orderCannotBeMadeToWrongAmount() {
    Long userId = 1L;
    String orderId = "임시orderId";
    Long wrongAmount = 25000L;
    OrderForDeliveryRequest request = createOrderForDeliveryRequest(wrongAmount);

    doNothing().when(feignHandler).validatePrice(any());
    doNothing().when(feignHandler).validatePurchaseDetails(any());
    KakaopayReadyResponseDto mockResponseDto = createKakaopayReadyResponseDto();
    when(feignHandler.ready(any())).thenReturn(mockResponseDto);
    when(orderUtil.generateUUID()).thenReturn(orderId);

    assertThatThrownBy(
            () ->
                orderService.readyForOrder(userId, request, OrderType.DELIVERY, OrderMethod.DIRECT))
        .isInstanceOf(InvalidOrderAmountException.class)
        .hasMessage("유효하지 않은 주문 금액입니다");
  }

  @Test
  @DisplayName("픽업 주문 - 준비단계 ")
  void readyForPickupOrder() {
    Long userId = 1L;
    String orderId = "임시orderId";

    OrderForPickupDto request = createOrderForPickupRequest();

    doNothing().when(feignHandler).validatePrice(any());
    doNothing().when(feignHandler).validatePurchaseDetails(any());
    KakaopayReadyResponseDto mockResponseDto = createKakaopayReadyResponseDto();
    when(feignHandler.ready(any())).thenReturn(mockResponseDto);
    when(orderUtil.generateUUID()).thenReturn(orderId);

    KakaopayReadyResponseDto responseDto =
        orderService.readyForPickupOrder(userId, request, OrderType.PICKUP);

    assertNotNull(responseDto);
  }

  @Test
  @DisplayName("구독 주문 - 준비단계")
  void readyForSubscriptionOrder() {
    Long userId = 1L;
    String orderId = "임시orderId";

    OrderForSubscriptionDto request = createOrderForSubscriptionRequest();

    doNothing().when(feignHandler).validatePrice(any());
    doNothing().when(feignHandler).validatePurchaseDetails(any());
    KakaopayReadyResponseDto mockResponseDto = createKakaopayReadyResponseDto();
    when(feignHandler.ready(any())).thenReturn(mockResponseDto);
    when(orderUtil.generateUUID()).thenReturn(orderId);

    KakaopayReadyResponseDto responseDto =
        orderService.readyForSubscriptionOrder(userId, request, OrderType.SUBSCRIBE);

    assertNotNull(responseDto);
  }

  @Test
  @DisplayName("바로주문, 장바구니 주문 - 타서비스 요청 단계")
  public void requestOrderForDelivery() {
    // given
    String orderId = "임시orderId";
    String orderType = OrderType.DELIVERY.toString();
    String pgToken = "임시pgToken";

    OrderInfo orderInfo = createOrderInfo(orderId, OrderType.valueOf(orderType), OrderMethod.CART);
    redisTemplate.opsForValue().set(orderId, orderInfo);

    orderService.requestOrder(orderId, orderType, pgToken);
    processOrderDtoKafkaProducer = mock(KafkaProducer.class);

    doNothing()
        .when(processOrderDtoKafkaProducer)
        .send(eq("coupon-use"), any(ProcessOrderDto.class));
    doNothing().when(processOrderDtoKafkaProducer).send(eq("delete-from-cart"), any());
  }

  @Test
  @DisplayName("픽업 주문 - 타서비스 요청 단계")
  public void requestOrderForPickup() {
    // given
    String orderId = "임시orderId";
    String orderType = OrderType.PICKUP.toString();
    String pgToken = "임시pgToken";

    PickupOrderInfo pickupOrderInfo = createPickupOrderInfo(orderId);
    redisTemplateForPickup.opsForValue().set(orderId, pickupOrderInfo);

    orderService.requestOrder(orderId, orderType, pgToken);
    processOrderDtoKafkaProducer = mock(KafkaProducer.class);

    doNothing()
        .when(processOrderDtoKafkaProducer)
        .send(eq("coupon-use"), any(ProcessOrderDto.class));
  }

  @Test
  @DisplayName("구독 주문 - 타서비스 요청 단계")
  public void requestOrderForSubscription() {
    // given
    String orderId = "임시orderId";
    String orderType = OrderType.SUBSCRIBE.toString();
    String pgToken = "임시pgToken";

    SubscriptionOrderInfo subscriptionOrderInfo = createSubscriptionOrderInfo(orderId);
    redisTemplateForSubscription.opsForValue().set(orderId, subscriptionOrderInfo);

    orderService.requestOrder(orderId, orderType, pgToken);
    processOrderDtoKafkaProducer = mock(KafkaProducer.class);

    doNothing()
        .when(processOrderDtoKafkaProducer)
        .send(eq("coupon-use"), any(ProcessOrderDto.class));
  }

  @Test
  @DisplayName("바로 주문하기 - 처리 및 저장 단계")
  void processOrderForDelivery() throws JsonProcessingException {
    // given
    String orderGroupId = "임시orderId";
    String orderDeliveryId = "임시가게주문id";

    OrderInfo orderInfo = createOrderInfo(orderGroupId, OrderType.DELIVERY, OrderMethod.DIRECT);
    redisTemplate.opsForValue().set(orderGroupId, orderInfo);

    when(feignHandler.createDelivery(any())).thenReturn(List.of(1L));
    when(feignHandler.approve(any())).thenReturn(LocalDateTime.now());
    when(orderUtil.generateUUID()).thenReturn(orderDeliveryId);

    ProcessOrderDto processOrderDto =
        OrderCommonMapper.toProcessOrderDto(orderGroupId, OrderType.DELIVERY.toString(), orderInfo);
    doNothing().when(feignHandler).createDeliveryAddress(any());
    doNothing().when(orderSNSPublisher).newOrderEventPublish(any());
    doNothing().when(orderSQSPublisher).publishOrderSuccess(any(), any());

    // when
    kafkaConsumer.processOrder(processOrderDto);
    // then
    List<OrderDelivery> orderDelivery = orderDeliveryRepository.findByOrderGroupId(orderGroupId);
    assertThat(orderDelivery).hasSize(1);
  }

  @Test
  @DisplayName("바로 주문하기 처리중에 예외 발생시 롤백 작업 진행")
  void rollbackForProcessOrderForDelivery() {
    // given
    String orderGroupId = "임시orderId";
    String orderDeliveryId = "임시가게주문id";
    String orderType = OrderType.DELIVERY.toString();

    OrderInfo orderInfo =
        createOrderInfo(orderGroupId, OrderType.valueOf(orderType), OrderMethod.DIRECT);
    redisTemplate.opsForValue().set(orderGroupId, orderInfo);

    when(feignHandler.createDelivery(any())).thenReturn(List.of(1L));
    when(feignHandler.approve(any())).thenReturn(LocalDateTime.now());
    when(orderUtil.generateUUID()).thenReturn(orderDeliveryId);

    ProcessOrderDto processOrderDto =
        OrderCommonMapper.toProcessOrderDto(orderGroupId, orderType, orderInfo);

    doNothing().when(processOrderDtoKafkaProducer).send("order-create-rollback", processOrderDto);

    // when
    doThrow(RuntimeException.class).when(feignHandler).createDeliveryAddress(any());
    kafkaConsumer.processOrder(processOrderDto);
    // then
    verify(processOrderDtoKafkaProducer).send("order-create-rollback", processOrderDto);
  }

  @Test
  @DisplayName("장바구니에서 주문하기 - 저장 및 처리단계")
  void processCartOrder() throws JsonProcessingException {
    // given
    String orderGroupId = "임시orderId";
    String orderDeliveryId = "임시가게주문id";
    OrderInfo orderInfo = createOrderInfo(orderGroupId, OrderType.DELIVERY, OrderMethod.CART);

    redisTemplate.opsForValue().set(orderGroupId, orderInfo);

    when(feignHandler.createDelivery(any())).thenReturn(List.of(1L));
    when(feignHandler.approve(any())).thenReturn(LocalDateTime.now());
    when(orderUtil.generateUUID()).thenReturn(orderDeliveryId);
    doNothing().when(processOrderDtoKafkaProducer).send(eq("delete-from-cart"), any());

    doNothing().when(feignHandler).createDeliveryAddress(any());
    doNothing().when(orderSNSPublisher).newOrderEventPublish(any());
    doNothing().when(orderSQSPublisher).publishOrderSuccess(any(), any());

    // when
    kafkaConsumer.processOrder(
        OrderCommonMapper.toProcessOrderDto(
            orderGroupId, OrderType.DELIVERY.toString(), orderInfo));
    // then
    List<OrderDelivery> orderDelivery = orderDeliveryRepository.findByOrderGroupId(orderGroupId);
    assertThat(orderDelivery).hasSize(1);
  }

  @Test
  @DisplayName("픽업 주문하기 - 저장 및 처리단계")
  void processPickupOrder() throws JsonProcessingException {
    // given
    String orderPickupId = "임시orderId";

    PickupOrderInfo pickupOrderInfo = createPickupOrderInfo(orderPickupId);

    redisTemplateForPickup.opsForValue().set(orderPickupId, pickupOrderInfo);

    when(feignHandler.createDelivery(any())).thenReturn(List.of(1L));
    when(paymentServiceClient.approve(any())).thenReturn(CommonResponse.success(null));
    doNothing().when(pickupCreateDtoKafkaProducer).send(eq("pickup-create"), any());

    // when
    kafkaConsumer.processOrder(
        OrderCommonMapper.toDtoForOrderPickup(orderPickupId, pickupOrderInfo));
    OrderPickup orderPickup =
        orderPickupRepository.findById(orderPickupId).orElseThrow(EntityNotFoundException::new);
    // then
    assertThat(orderPickup.getOrderPickupId()).isEqualTo(orderPickupId);

    doNothing().when(feignHandler).createDeliveryAddress(any());
    doNothing().when(orderSNSPublisher).newOrderEventPublish(any());
    doNothing().when(orderSQSPublisher).publishOrderSuccess(any(), any());
  }

  @Test
  @DisplayName("구독 주문하기 - 저장 및 처리단계")
  void processSubscriptionOrder() {
    String orderSubscriptionId = "임시orderId";
    SubscriptionOrderInfo subscriptionOrderInfo = createSubscriptionOrderInfo(orderSubscriptionId);
    redisTemplateForSubscription.opsForValue().set(orderSubscriptionId, subscriptionOrderInfo);

    when(feignHandler.createDelivery(any())).thenReturn(List.of(1L));
    when(feignHandler.approve(any())).thenReturn(LocalDateTime.now());

    doNothing().when(subscriptionCreateDtoKafkaProducer).send(eq("subscription-create"), any());

    // when
    doNothing().when(feignHandler).createDeliveryAddress(any());
    doNothing().when(orderSNSPublisher).newOrderEventPublish(any());
    doNothing().when(orderSQSPublisher).publishOrderSuccess(any(), any());

    kafkaConsumer.processOrder(
        OrderCommonMapper.toDtoForOrderSubscription(orderSubscriptionId, subscriptionOrderInfo));
    OrderSubscription orderSubscription =
        orderSubscriptionRepository
            .findById(orderSubscriptionId)
            .orElseThrow(EntityNotFoundException::new);
    // then
    assertThat(orderSubscription.getOrderSubscriptionId()).isEqualTo(orderSubscriptionId);
  }

  @Test
  @DisplayName("주문에 대한 결제는 5분 안에 이뤄져야한다.")
  // 오류 발생시, kafka를 통해 롤백하고 오류를 SNS를 통해 보낸다.
  void paymentShouldBeWithinTimeLimit() throws JsonProcessingException {
    // given
    String orderGroupId = "임시orderId";
    String orderType = OrderType.DELIVERY.toString();
    OrderInfo orderInfo = createOrderInfo(orderGroupId, OrderType.DELIVERY, OrderMethod.DIRECT);

    when(feignHandler.createDelivery(any())).thenReturn(List.of(1L));

    doNothing()
        .when(processOrderDtoKafkaProducer)
        .send(eq("order-create-rollback"), any(ProcessOrderDto.class));

    ProcessOrderDto processOrderDto =
        OrderCommonMapper.toProcessOrderDto(orderGroupId, orderType, orderInfo);
    // when
    kafkaConsumer.processOrder(processOrderDto);

    // then
    verify(processOrderDtoKafkaProducer).send("order-create-rollback", processOrderDto);
  }

  @Test
  @DisplayName("배송 상태를 kafka를 통해 넘겨받는다")
  void updateOrderStatus() {
    UpdateOrderStatusDto updateOrderStatusDto =
        UpdateOrderStatusDto.builder()
            .orderDeliveryId("가게주문id")
            .phoneNumber("01011112222")
            .deliveryStatus(DeliveryStatus.COMPLETED)
            .build();

    kafkaConsumer.updateOrderDeliveryStatus(updateOrderStatusDto);
    doNothing().when(orderSQSPublisher).publishDeliveryNotification(any(),any());
    OrderDelivery orderDelivery =
        orderDeliveryRepository.findById("가게주문id").orElseThrow(EntityNotFoundException::new);

    assertThat(orderDelivery.getOrderDeliveryStatus().toString()).isEqualTo("COMPLETED");
  }

  @Test
  @DisplayName("배치를 통해 매달 정기결제 진행")
  void processBatchSubscription() {
    SubscriptionBatchDto subscriptionBatchDto = SubscriptionBatchDto.builder()
            .orderSubscriptionId("orderSubscriptionId_1")
            .userId(1L)
            .build();
    SubscriptionBatchDtoList subscriptionBatchDtoList =
            SubscriptionBatchDtoList.builder()
            .subscriptionBatchDtoList(List.of(subscriptionBatchDto))
            .build();

    doNothing().when(feignHandler).processSubscription(subscriptionBatchDtoList);
    doNothing().when(orderSNSPublisher).newOrderEventPublish(any());
    doNothing().when(orderSQSPublisher).publishOrderSuccess(any(), any());
    doNothing()
        .when(subscriptionDateDtoListKafkaProducer)
        .send(eq("subscription-date-update"), any());

    orderService.processSubscriptionBatch(subscriptionBatchDtoList);
  }

  @Test
  @DisplayName("픽업상태 변경")
  void changePickupStatus() {
    LocalDateTime now = LocalDateTime.now();

    OrderPickupStatus orderPickupStatus = OrderPickupStatus.COMPLETED;
    orderService.pickupStatusChange(now, orderPickupStatus);
    doNothing().when(pickupStatusUpdateKafkaProducer).send(eq("pickup-status-update"), any());
  }

  @Test
  @DisplayName("구독상태 변경")
  void changeSubscriptionStatus(){
    UpdateOrderSubscriptionStatusDto updateOrderSubscriptionStatusDto = UpdateOrderSubscriptionStatusDto.builder()
            .deliveryIds(List.of(1L,2L))
            .build();

    doNothing().when(subscriptionStatusChangeDtoKafkaProducer).send(eq("subscription-status-update"), any());
    orderService.updateOrderSubscriptionStatus(updateOrderSubscriptionStatusDto);
  }

  public OrderForDeliveryRequest createOrderForDeliveryRequest(Long sumOfActualAmount) {
    return OrderForDeliveryRequest.builder()
        .orderInfoByStores(createOrderInfoByStores())
        .sumOfActualAmount(sumOfActualAmount)
        .ordererName("주문자 이름")
        .ordererPhoneNumber("주문자 전화번호")
        .ordererEmail("주문자 이메일")
        .recipientName("수신자 이름")
        .deliveryZipcode("우편번호")
        .deliveryRoadName("도로명")
        .deliveryAddressDetail("상세주소")
        .recipientPhone("수신자 전화번호")
        .deliveryRequest("배송 요청사항")
        .build();
  }

  public List<OrderInfoByStore> createOrderInfoByStores() {
    List<ProductCreate> productList = createProductCreates();
    List<OrderInfoByStore> list = new ArrayList<>();

    OrderInfoByStore orderInfoByStore =
        OrderInfoByStore.builder()
            .storeId(1L)
            .products(productList)
            .totalAmount(90000L)
            .deliveryCost(3500L)
            .couponId(1L)
            .couponAmount(3000L)
            .actualAmount(90500L)
            .build();
    list.add(orderInfoByStore);
    return list;
  }

  public List<ProductCreate> createProductCreates() {
    List<ProductCreate> list = new ArrayList<>();
    list.add(
        ProductCreate.builder()
            .productId("1")
            .productName("상품명")
            .quantity(2L)
            .price(35000L)
            .productThumbnailImage("썸네일이미지url")
            .build());
    list.add(
        ProductCreate.builder()
            .productId("2")
            .productName("상품명")
            .quantity(1L)
            .price(20000L)
            .productThumbnailImage("썸네일이미지url")
            .build());
    return list;
  }

  public KakaopayReadyResponseDto createKakaopayReadyResponseDto() {
    return KakaopayReadyResponseDto.builder()
        .nextRedirectPcUrl("임시redirectUrl주소")
        .tid("임시tid코드")
        .build();
  }

  private OrderForPickupDto createOrderForPickupRequest() {
    ProductCreate productCreate =
        ProductCreate.builder()
            .productId("1")
            .productName("상품명")
            .quantity(2L)
            .price(35000L)
            .productThumbnailImage("썸네일이미지url")
            .build();

    return OrderForPickupDto.builder()
        .storeId(1L)
        .storeName("가게이름")
        .pickupDate("2023-12-23")
        .pickupTime("12:30")
        .product(productCreate)
        .totalAmount(70000L)
        .deliveryCost(0L)
        .couponId(1L)
        .couponAmount(2000L)
        .actualAmount(68000L)
        .ordererName("주문자 이름")
        .ordererPhoneNumber("주문자 전화번호")
        .ordererEmail("주문자 이메일")
        .build();
  }

  private OrderForSubscriptionDto createOrderForSubscriptionRequest() {
    ProductCreate productCreate =
        ProductCreate.builder()
            .productId("1")
            .productName("상품명")
            .quantity(2L)
            .price(35000L)
            .productThumbnailImage("썸네일이미지url")
            .build();
    return OrderForSubscriptionDto.builder()
        .storeId(1L)
        .storeName("가게이름")
        .paymentDay(LocalDate.now())
        .deliveryDay(LocalDate.now().plusDays(3))
        .products(productCreate)
        .totalAmount(70000L)
        .deliveryCost(0L)
        .couponId(1L)
        .couponAmount(2000L)
        .actualAmount(68000L)
        .ordererName("주문자 이름")
        .ordererPhoneNumber("주문자 전화번호")
        .ordererEmail("주문자 이메일")
        .recipientName("수신자")
        .deliveryZipcode("우편번호")
        .deliveryRoadName("도로명주소")
        .deliveryAddressDetail("상세주소")
        .recipientPhone("수신자 전화번호")
        .deliveryRequest("배송 요청사항")
        .deliveryAddressId(1L)
        .build();
  }

  public OrderInfo createOrderInfo(String orderId, OrderType orderType, OrderMethod orderMethod) {
    return OrderInfo.builder()
        .tempOrderId(orderId)
        .userId(1L)
        .itemName("상품명 외 1건")
        .sumOfAllQuantity(2L)
        .orderInfoByStores(createOrderInfoByStores())
        .sumOfActualAmount(98000L)
        .isSubscriptionPay(false)
        .ordererName("주문자명")
        .ordererPhoneNumber("주문자 전화번호")
        .ordererEmail("주문자 이메일")
        .recipientName("수신자명")
        .deliveryZipcode("우편번호")
        .deliveryAddressDetail("상세주소")
        .recipientPhone("수신자 전화번호")
        .deliveryRequest("배송 요청사항")
        .tid("tid고유번호")
        .orderType(orderType.toString())
        .orderMethod(orderMethod.toString())
        .build();
  }

  public PickupOrderInfo createPickupOrderInfo(String orderId) {
    ProductCreate productCreate =
        ProductCreate.builder()
            .productId("1")
            .productName("상품명")
            .quantity(2L)
            .price(35000L)
            .productThumbnailImage("썸네일이미지url")
            .build();

    return PickupOrderInfo.builder()
        .tempOrderId(orderId)
        .userId(1L)
        .itemName("상품명 외 1건")
        .quantity(2L)
        .storeId(1L)
        .storeName("가게이름")
        .pickupDate("2023-12-23")
        .pickupTime("13:20")
        .product(productCreate)
        .totalAmount(50000L)
        .deliveryCost(5000L)
        .couponId(null)
        .couponAmount(0L)
        .actualAmount(45000L)
        .isSubscriptionPay(false)
        .ordererName("주문자명")
        .ordererPhoneNumber("주문자 전화번호")
        .ordererEmail("주문자 이메일")
        .tid("tid고유번호")
        .orderType(OrderType.PICKUP.toString())
        .build();
  }

  public SubscriptionOrderInfo createSubscriptionOrderInfo(String orderId) {
    ProductCreate productCreate =
        ProductCreate.builder()
            .productId("1")
            .productName("상품명")
            .quantity(2L)
            .price(35000L)
            .productThumbnailImage("썸네일이미지url")
            .build();

    return SubscriptionOrderInfo.builder()
        .tempOrderId(orderId)
        .userId(1L)
        .itemName("상품명 외 1건")
        .quantity(2L)
        .storeId(1L)
        .storeName("가게이름")
        .product(productCreate)
        .totalAmount(50000L)
        .deliveryCost(5000L)
        .couponId(0L)
        .couponAmount(0L)
        .actualAmount(45000L)
        .isSubscriptionPay(true)
        .ordererName("주문자명")
        .ordererPhoneNumber("주문자 전화번호")
        .ordererEmail("주문자 이메일")
        .recipientName("수신자명")
        .deliveryZipcode("우편번호")
        .deliveryRoadName("도로명주소")
        .deliveryAddressDetail("상세주소")
        .recipientPhone("전화번호")
        .deliveryRequest("요청사항")
        .deliveryAddressId(1L)
        .tid("tid고유번호")
        .orderType(OrderType.SUBSCRIBE.toString())
        .build();
  }

  private OrderPickup createOrderPickupWithDateTime(LocalDateTime orderPickupDateTime) {
    return OrderPickup.builder()
            .orderPickupId(UUID.randomUUID().toString())
//            .orderPickupProduct()
            .userId(1L)
            .storeId(1L)
            .orderPickupTotalAmount(1000L)
            .orderPickupCouponAmount(10000L)
            .orderPickupDatetime(orderPickupDateTime)
            .orderPickupPhoneNumber("12345")
            .build();
  }
}
