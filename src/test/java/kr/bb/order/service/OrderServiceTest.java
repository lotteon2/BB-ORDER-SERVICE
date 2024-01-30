package kr.bb.order.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityNotFoundException;
import kr.bb.order.dto.request.orderForDelivery.OrderForDeliveryRequest;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.entity.pickup.OrderPickup;
import kr.bb.order.entity.pickup.OrderPickupStatus;
import kr.bb.order.entity.redis.OrderInfo;
import kr.bb.order.entity.redis.PickupOrderInfo;
import kr.bb.order.entity.redis.SubscriptionOrderInfo;
import kr.bb.order.entity.subscription.OrderSubscription;
import kr.bb.order.facade.OrderFacade;
import kr.bb.order.feign.*;
import kr.bb.order.infra.OrderSNSPublisher;
import kr.bb.order.infra.OrderSQSPublisher;
import kr.bb.order.kafka.KafkaConsumer;
import kr.bb.order.kafka.KafkaProducer;
import kr.bb.order.kafka.SubscriptionDateDtoList;
import kr.bb.order.mapper.OrderCommonMapper;
import kr.bb.order.repository.*;
import kr.bb.order.util.RedisOperation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@Transactional
class OrderServiceTest extends AbstractContainerBaseTest {
  @Autowired private OrderFacade orderFacade;
  @MockBean private PaymentServiceClient paymentServiceClient;
  @Autowired private OrderDeliveryRepository orderDeliveryRepository;
  @MockBean private KafkaProducer<ProcessOrderDto> processOrderDtoKafkaProducer;
  @MockBean private KafkaProducer<CartDeleteCommand> cartItemDeleteKafkaProducer;
  @MockBean private KafkaProducer<PickupCreateDto> pickupCreateDtoKafkaProducer;
  @MockBean private KafkaProducer<SubscriptionCreateDto> subscriptionCreateDtoKafkaProducer;
  @Autowired private KafkaConsumer<ProcessOrderDto> kafkaConsumer;
  @MockBean private KafkaProducer<SubscriptionDateDtoList> subscriptionDateDtoListKafkaProducer;
  @MockBean private KafkaProducer<SubscriptionStatusChangeDto> subscriptionStatusChangeDtoKafkaProducer;
  @Autowired private OrderPickupRepository orderPickupRepository;
  @Autowired private OrderSubscriptionRepository orderSubscriptionRepository;
  @MockBean private OrderSNSPublisher orderSNSPublisher;
  @MockBean private OrderSQSPublisher orderSQSPublisher;
  @MockBean private FeignHandler feignHandler;
  @MockBean private SimpleMessageListenerContainer simpleMessageListenerContainer;
  @MockBean private KafkaProducer<PickupStatusChangeDto> pickupStatusUpdateKafkaProducer;
  @Autowired private RedisOperation redisOperation;

  @Test
  @DisplayName("바로 주문하기 - 처리 및 저장 단계")
  void processOrderForDelivery() {
    // given
    String orderGroupId = "임시orderId";

    OrderInfo orderInfo = createOrderInfo(orderGroupId, OrderType.DELIVERY, OrderMethod.DIRECT);
    redisOperation.saveIntoRedis(orderGroupId, orderInfo);

    when(feignHandler.createDelivery(any())).thenReturn(List.of(1L));
    when(feignHandler.approve(any())).thenReturn(LocalDateTime.now());

    ProcessOrderDto processOrderDto =
        OrderCommonMapper.toProcessOrderDto(orderGroupId, OrderType.DELIVERY.toString(), orderInfo);
    doNothing().when(feignHandler).createDeliveryAddress(any());
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
    String orderType = OrderType.DELIVERY.toString();

    OrderInfo orderInfo =
        createOrderInfo(orderGroupId, OrderType.valueOf(orderType), OrderMethod.DIRECT);
    redisOperation.saveIntoRedis(orderGroupId, orderInfo);

    when(feignHandler.createDelivery(any())).thenReturn(List.of(1L));
    when(feignHandler.approve(any())).thenReturn(LocalDateTime.now());

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
  void processCartOrder() {
    // given
    String orderGroupId = "임시orderId";
    OrderInfo orderInfo = createOrderInfo(orderGroupId, OrderType.DELIVERY, OrderMethod.CART);

    redisOperation.saveIntoRedis(orderGroupId, orderInfo);

    when(feignHandler.createDelivery(any())).thenReturn(List.of(1L));
    when(feignHandler.approve(any())).thenReturn(LocalDateTime.now());
    doNothing().when(cartItemDeleteKafkaProducer).send(eq("delete-from-cart"), any());

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
  void processPickupOrder() {
    // given
    String orderPickupId = "임시orderId";

    PickupOrderInfo pickupOrderInfo = createPickupOrderInfo(orderPickupId);

    redisOperation.saveIntoRedis(orderPickupId, pickupOrderInfo);

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
    redisOperation.saveIntoRedis(orderSubscriptionId, subscriptionOrderInfo);

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
  void paymentShouldBeWithinTimeLimit() {
    // given
    String orderGroupId = "임시orderId";
    String orderType = OrderType.DELIVERY.toString();
    OrderInfo orderInfo = createOrderInfo(orderGroupId, OrderType.DELIVERY, OrderMethod.DIRECT);

    when(feignHandler.createDelivery(any())).thenReturn(List.of(1L));

    doNothing().when(orderSQSPublisher).publishOrderFail(any(), any());
    doNothing()
        .when(processOrderDtoKafkaProducer)
        .send(eq("order-create-rollback"), any(ProcessOrderDto.class));

    ProcessOrderDto processOrderDto =
        OrderCommonMapper.toProcessOrderDto(orderGroupId, orderType, orderInfo);
    // when
    kafkaConsumer.processOrder(processOrderDto);

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
    UpdateOrderStatusDto updateOrderStatusDto2 =
            UpdateOrderStatusDto.builder()
                    .orderDeliveryId("가게주문id2")
                    .phoneNumber("01011112222")
                    .deliveryStatus(DeliveryStatus.PROCESSING)
                    .build();

    kafkaConsumer.updateOrderDeliveryStatus(updateOrderStatusDto);
    kafkaConsumer.updateOrderDeliveryStatus(updateOrderStatusDto2);
    doNothing().when(orderSQSPublisher).publishDeliveryNotification(any(),any());
    OrderDelivery orderDelivery =
        orderDeliveryRepository.findById("가게주문id").orElseThrow(EntityNotFoundException::new);
    OrderDelivery orderDelivery2 =
        orderDeliveryRepository.findById("가게주문id2").orElseThrow(EntityNotFoundException::new);

    assertThat(orderDelivery.getOrderDeliveryStatus()).isEqualTo(DeliveryStatus.COMPLETED);
    assertThat(orderDelivery2.getOrderDeliveryStatus()).isEqualTo(DeliveryStatus.PROCESSING);
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

    orderFacade.processSubscriptionBatch(subscriptionBatchDtoList);
  }

  @Test
  @DisplayName("픽업상태 변경")
  void changePickupStatus() {
    LocalDateTime now = LocalDateTime.now();

    OrderPickupStatus orderPickupStatus = OrderPickupStatus.COMPLETED;
    orderFacade.pickupStatusChange(now, orderPickupStatus);
    doNothing().when(pickupStatusUpdateKafkaProducer).send(eq("pickup-status-update"), any());
  }

  @Test
  @DisplayName("구독상태 변경")
  void changeSubscriptionStatus(){
    UpdateOrderSubscriptionStatusDto updateOrderSubscriptionStatusDto = UpdateOrderSubscriptionStatusDto.builder()
            .deliveryIds(List.of(1L,2L))
            .build();

    doNothing().when(subscriptionStatusChangeDtoKafkaProducer).send(eq("subscription-status-update"), any());
    orderFacade.updateOrderSubscriptionStatus(updateOrderSubscriptionStatusDto);
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

  public static List<OrderInfoByStore> createOrderInfoByStores() {
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

  public static List<ProductCreate> createProductCreates() {
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

  public static OrderInfo createOrderInfo(String orderId, OrderType orderType,
          OrderMethod orderMethod) {
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

  public static PickupOrderInfo createPickupOrderInfo(String orderId) {
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

  public static SubscriptionOrderInfo createSubscriptionOrderInfo(String orderId) {
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
}
