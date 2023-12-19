package kr.bb.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bloomingblooms.response.CommonResponse;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityNotFoundException;
import kr.bb.order.dto.request.orderForDelivery.OrderForDeliveryRequest;
import kr.bb.order.dto.request.orderForDelivery.OrderInfoByStore;
import kr.bb.order.dto.request.orderForDelivery.ProductCreate;
import kr.bb.order.dto.request.orderForPickup.OrderForPickupDto;
import kr.bb.order.dto.request.store.ProcessOrderDto;
import kr.bb.order.dto.response.payment.KakaopayReadyResponseDto;
import kr.bb.order.entity.OrderType;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.entity.pickup.OrderPickup;
import kr.bb.order.entity.redis.OrderInfo;
import kr.bb.order.entity.redis.PickupOrderInfo;
import kr.bb.order.exception.InvalidOrderAmountException;
import kr.bb.order.exception.PaymentExpiredException;
import kr.bb.order.feign.DeliveryServiceClient;
import kr.bb.order.feign.PaymentServiceClient;
import kr.bb.order.feign.ProductServiceClient;
import kr.bb.order.feign.StoreServiceClient;
import kr.bb.order.kafka.KafkaConsumer;
import kr.bb.order.kafka.KafkaProducer;
import kr.bb.order.repository.OrderDeliveryRepository;
import kr.bb.order.repository.OrderGroupRepository;
import kr.bb.order.repository.OrderPickupRepository;
import kr.bb.order.repository.OrderProductRepository;
import kr.bb.order.util.OrderUtil;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@Testcontainers
@Transactional
class OrderServiceTest extends AbstractContainerBaseTest {
  @Autowired private OrderService orderService;
  @MockBean private ProductServiceClient productServiceClient;
  @MockBean private StoreServiceClient storeServiceClient;
  @MockBean private PaymentServiceClient paymentServiceClient;
  @Autowired private RedisTemplate<String, OrderInfo> redisTemplate;
  @Autowired private RedisTemplate<String, PickupOrderInfo> redisTemplateForPickup;
  @Autowired private OrderManager orderManager;
  @Autowired private OrderDeliveryRepository orderDeliveryRepository;
  @MockBean private DeliveryServiceClient deliveryServiceClient;
  @MockBean private KafkaProducer kafkaProducer;
  @Autowired private KafkaConsumer kafkaConsumer;
  @MockBean private OrderUtil orderUtil;
  @Autowired private OrderProductRepository orderProductRepository;
  @Autowired private OrderGroupRepository orderGroupRepository;
  @Autowired private OrderPickupRepository orderPickupRepository;

  @BeforeEach
  void setup() {
    orderService =
        new OrderService(
            productServiceClient,
            storeServiceClient,
            paymentServiceClient,
            redisTemplate,
            redisTemplateForPickup,
            orderManager,
            orderDeliveryRepository,
            deliveryServiceClient,
            kafkaProducer,
            orderUtil,
            orderProductRepository,
            orderGroupRepository,
            orderPickupRepository);
  }

//  @Autowired
//  void setOrderService(OrderService orderService){
//    this.orderService = orderService;
//  }

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

    when(productServiceClient.validatePrice(any())).thenReturn(CommonResponse.success(null));
    when(storeServiceClient.validatePurchaseDetails(any()))
        .thenReturn(CommonResponse.success(null));
    KakaopayReadyResponseDto mockResponseDto = createKakaopayReadyResponseDto();
    when(paymentServiceClient.ready(any())).thenReturn(CommonResponse.success(mockResponseDto));
    when(orderUtil.generateUUID()).thenReturn(orderId);

    KakaopayReadyResponseDto responseDto =
        orderService.readyForOrder(userId, request, OrderType.ORDER_DELIVERY);

    assertNotNull(responseDto);
  }

  @Test
  @DisplayName("잘못된 결제금액으로는 주문이 불가능하다")
  public void orderCannotBeMadeToWrongAmount() {
    Long userId = 1L;
    String orderId = "임시orderId";
    Long wrongAmount = 25000L;
    OrderForDeliveryRequest request = createOrderForDeliveryRequest(wrongAmount);

    when(productServiceClient.validatePrice(any())).thenReturn(CommonResponse.success(null));
    when(storeServiceClient.validatePurchaseDetails(any()))
        .thenReturn(CommonResponse.success(null));
    KakaopayReadyResponseDto mockResponseDto = createKakaopayReadyResponseDto();
    when(paymentServiceClient.ready(any())).thenReturn(CommonResponse.success(mockResponseDto));
    when(orderUtil.generateUUID()).thenReturn(orderId);

    assertThatThrownBy(() -> orderService.readyForOrder(userId, request, OrderType.ORDER_DELIVERY))
        .isInstanceOf(InvalidOrderAmountException.class)
        .hasMessage("유효하지 않은 주문 금액입니다");
  }

  @Test
  @DisplayName("픽업 주문 - 준비단계 ")
  void readyForPickupOrder() {
    Long userId = 1L;
    String orderId = "임시orderId";

    OrderForPickupDto request = createOrderForPickupRequest();

    when(productServiceClient.validatePrice(any())).thenReturn(CommonResponse.success(null));
    when(storeServiceClient.validatePurchaseDetails(any()))
        .thenReturn(CommonResponse.success(null));
    KakaopayReadyResponseDto mockResponseDto = createKakaopayReadyResponseDto();
    when(paymentServiceClient.ready(any())).thenReturn(CommonResponse.success(mockResponseDto));
    when(orderUtil.generateUUID()).thenReturn(orderId);

    KakaopayReadyResponseDto responseDto =
        orderService.readyForPickupOrder(userId, request, OrderType.ORDER_PICKUP);

    assertNotNull(responseDto);
  }

  @Test
  @DisplayName("바로주문, 장바구니 주문 - 타서비스 요청 단계")
  public void requestOrderForDelivery() {
    // given
    String orderId = "임시orderId";
    String orderType = OrderType.ORDER_DELIVERY.toString();
    String pgToken = "임시pgToken";

    OrderInfo orderInfo = createOrderInfo(orderId);
    redisTemplate.opsForValue().set(orderId, orderInfo);

    orderService.requestOrder(orderId, orderType, pgToken);
    kafkaProducer = mock(KafkaProducer.class);

    doNothing().when(kafkaProducer).requestOrder(any(ProcessOrderDto.class));
    doNothing().when(kafkaProducer).deleteFromCart(any());
  }

  @Test
  @DisplayName("픽업 주문 - 타서비스 요청 단계")
  public void requestOrderForPickup() {
    // given
    String orderId = "임시orderId";
    String orderType = OrderType.ORDER_PICKUP.toString();
    String pgToken = "임시pgToken";

    PickupOrderInfo pickupOrderInfo = createPickupOrderInfo(orderId);
    redisTemplateForPickup.opsForValue().set(orderId, pickupOrderInfo);

    orderService.requestOrder(orderId, orderType, pgToken);
    kafkaProducer = mock(KafkaProducer.class);

    doNothing().when(kafkaProducer).requestOrder(any(ProcessOrderDto.class));
  }

  @Test
  @DisplayName("바로 주문하기 - 처리 및 저장 단계")
  void processOrderForDelivery() throws JsonProcessingException {
    // TODO: kafka consumer를 실행시켜 테스트하는 방법 찾아보기
    // given
    Long userId = 1L;
    String orderGroupId = "임시orderId";
    String orderDeliveryId = "임시가게주문id";
    String orderType = OrderType.ORDER_DELIVERY.toString();

    List<OrderInfoByStore> orderInfoByStores = createOrderInfoByStores();
    ObjectMapper objectMapper = new ObjectMapper();
    String message =
        objectMapper.writeValueAsString(
            ProcessOrderDto.toDtoForOrderDelivery(orderGroupId, orderType, orderInfoByStores));
    OrderForDeliveryRequest requestDto = createOrderForDeliveryRequest(90500L);

    OrderInfo orderInfo =
        OrderInfo.transformDataForApi(
            orderGroupId,
            userId,
            "제품명 외 1개",
            2,
            false,
            "tid번호",
            requestDto,
            OrderType.ORDER_DELIVERY);
    redisTemplate.opsForValue().set(orderGroupId, orderInfo);

    List<Long> deliveryIds = new ArrayList<>();
    deliveryIds.add(1L);
    CommonResponse<List<Long>> success = CommonResponse.success(deliveryIds);

    when(deliveryServiceClient.createDelivery(any())).thenReturn(success);
    when(paymentServiceClient.approve(any())).thenReturn(CommonResponse.success(null));
    when(orderUtil.generateUUID()).thenReturn(orderDeliveryId);

    // when
    kafkaConsumer.processOrder(message);
    // then
    List<OrderDelivery> orderDelivery = orderDeliveryRepository.findByOrderGroupId(orderGroupId);
    assertThat(orderDelivery).hasSize(1);
  }

  @Test
  @DisplayName("장바구니에서 주문하기 - 저장 및 처리단계")
  void processCartOrder() throws JsonProcessingException {
    // TODO: kafka consumer를 실행시켜 테스트하는 방법 찾아보기
    // given
    Long userId = 1L;
    String orderGroupId = "임시orderId";
    String orderDeliveryId = "임시가게주문id";
    String orderType = OrderType.ORDER_DELIVERY.toString();

    List<OrderInfoByStore> orderInfoByStores = createOrderInfoByStores();
    ObjectMapper objectMapper = new ObjectMapper();
    String message =
        objectMapper.writeValueAsString(
            ProcessOrderDto.toDtoForOrderDelivery(orderGroupId, orderType, orderInfoByStores));
    OrderForDeliveryRequest requestDto = createOrderForDeliveryRequest(90500L);

    OrderInfo orderInfo =
        OrderInfo.transformDataForApi(
            orderGroupId, userId, "제품명 외 1개", 2, false, "tid번호", requestDto, OrderType.ORDER_CART);
    redisTemplate.opsForValue().set(orderGroupId, orderInfo);

    List<Long> deliveryIds = new ArrayList<>();
    deliveryIds.add(1L);
    CommonResponse<List<Long>> success = CommonResponse.success(deliveryIds);

    when(deliveryServiceClient.createDelivery(any())).thenReturn(success);
    when(paymentServiceClient.approve(any())).thenReturn(CommonResponse.success(null));
    when(orderUtil.generateUUID()).thenReturn(orderDeliveryId);
    doNothing().when(kafkaProducer).deleteFromCart(any());

    // when
    kafkaConsumer.processOrder(message);
    // then
    List<OrderDelivery> orderDelivery = orderDeliveryRepository.findByOrderGroupId(orderGroupId);
    assertThat(orderDelivery).hasSize(1);
  }

  @Test
  @DisplayName("픽업 주문하기 - 저장 및 처리단계")
  void processPickupOrder() throws JsonProcessingException {
    // TODO: kafka consumer를 실행시켜 테스트하는 방법 찾아보기
    // given
    String orderId = "임시orderId";
    String orderDeliveryId = "임시가게주문id";

    PickupOrderInfo pickupOrderInfo = createPickupOrderInfo(orderId);
    ObjectMapper objectMapper = new ObjectMapper();
    String message =
        objectMapper.writeValueAsString(
            ProcessOrderDto.toDtoForOrderPickup(orderId, pickupOrderInfo));
    OrderForDeliveryRequest requestDto = createOrderForDeliveryRequest(90500L);

    redisTemplateForPickup.opsForValue().set(orderId, pickupOrderInfo);

    List<Long> deliveryIds = new ArrayList<>();
    deliveryIds.add(1L);
    CommonResponse<List<Long>> success = CommonResponse.success(deliveryIds);

    when(deliveryServiceClient.createDelivery(any())).thenReturn(success);
    when(paymentServiceClient.approve(any())).thenReturn(CommonResponse.success(null));
    when(orderUtil.generateUUID()).thenReturn(orderDeliveryId);
    doNothing().when(kafkaProducer).deleteFromCart(any());

    // when
    kafkaConsumer.processOrder(message);
    OrderPickup orderPickup =
        orderPickupRepository.findById(orderId).orElseThrow(EntityNotFoundException::new);
    // then
    assertThat(orderPickup.getOrderPickupId()).isEqualTo(orderId);
  }

  @Test
  @DisplayName("주문에 대한 결제는 5분 안에 이뤄져야한다.")
  void paymentShouldBeWithinTimeLimit() throws JsonProcessingException {
    // TODO: kafka consumer를 실행시켜 테스트하는 방법 찾아보기
    // given
    String orderGroupId = "임시orderId";
    List<OrderInfoByStore> orderInfoByStores = createOrderInfoByStores();
    String orderType = OrderType.ORDER_DELIVERY.toString();
    ObjectMapper objectMapper = new ObjectMapper();
    String message =
        objectMapper.writeValueAsString(
            ProcessOrderDto.toDtoForOrderDelivery(orderGroupId, orderType, orderInfoByStores));
    List<Long> deliveryIds = new ArrayList<>();
    deliveryIds.add(1L);
    CommonResponse<List<Long>> success = CommonResponse.success(deliveryIds);

    when(deliveryServiceClient.createDelivery(any())).thenReturn(success);

    kafkaProducer = mock(KafkaProducer.class);
    doNothing().when(kafkaProducer).rollbackOrder(any(ProcessOrderDto.class));

    // when, then
    assertThatThrownBy(() -> kafkaConsumer.processOrder(message))
        .isInstanceOf(PaymentExpiredException.class)
        .hasMessage("결제 시간이 만료되었습니다. 다시 시도해주세요.");
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

  public OrderInfo createOrderInfo(String orderId) {
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
        .orderType(OrderType.ORDER_DELIVERY.toString())
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
        .couponId(0L)
        .couponAmount(0L)
        .actualAmount(45000L)
        .isSubscriptionPay(false)
        .ordererName("주문자명")
        .ordererPhoneNumber("주문자 전화번호")
        .ordererEmail("주문자 이메일")
        .tid("tid고유번호")
        .orderType(OrderType.ORDER_PICKUP.toString())
        .build();
  }
}
