package kr.bb.order.service.settlement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bloomingblooms.domain.notification.order.OrderType;
import bloomingblooms.domain.order.OrderInfoByStore;
import bloomingblooms.domain.order.OrderMethod;
import bloomingblooms.domain.order.ProcessOrderDto;
import bloomingblooms.domain.order.ProductCreate;
import bloomingblooms.domain.payment.KakaopayReadyResponseDto;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import kr.bb.order.dto.request.orderForDelivery.OrderForDeliveryRequest;
import kr.bb.order.dto.request.orderForPickup.OrderForPickupDto;
import kr.bb.order.dto.request.orderForSubscription.OrderForSubscriptionDto;
import kr.bb.order.entity.redis.OrderInfo;
import kr.bb.order.entity.redis.PickupOrderInfo;
import kr.bb.order.entity.redis.SubscriptionOrderInfo;
import kr.bb.order.exception.InvalidOrderAmountException;
import kr.bb.order.facade.OrderFacade;
import kr.bb.order.feign.FeignHandler;
import kr.bb.order.kafka.KafkaProducer;
import kr.bb.order.service.AbstractContainerBaseTest;
import kr.bb.order.util.RedisOperation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
public class OrderFacadeTest extends AbstractContainerBaseTest {
  @Autowired OrderFacade orderFacade;
  @MockBean private FeignHandler feignHandler;
  @Autowired private RedisOperation redisOperation;
  @MockBean private SimpleMessageListenerContainer simpleMessageListenerContainer;
  @MockBean private KafkaProducer<ProcessOrderDto> processOrderDtoKafkaProducer;

  @Test
  @DisplayName("바로 주문하기 - 준비단계")
  public void readyForDeliveryOrder() {
    Long userId = 1L;
    Long sumOfActualAmount = 90500L;
    OrderForDeliveryRequest request = createOrderForDeliveryRequest(sumOfActualAmount);

    doNothing().when(feignHandler).validatePrice(any());
    doNothing().when(feignHandler).validatePurchaseDetails(any());
    KakaopayReadyResponseDto mockResponseDto = createKakaopayReadyResponseDto();
    when(feignHandler.ready(any())).thenReturn(mockResponseDto);

    KakaopayReadyResponseDto responseDto =
        orderFacade.readyForOrder(userId, request, OrderType.DELIVERY, OrderMethod.DIRECT);

    assertNotNull(responseDto);
  }

  @Test
  @DisplayName("잘못된 결제금액으로는 주문이 불가능하다")
  public void orderCannotBeMadeToWrongAmount() {
    Long userId = 1L;
    Long wrongAmount = 25000L;
    OrderForDeliveryRequest request = createOrderForDeliveryRequest(wrongAmount);

    doNothing().when(feignHandler).validatePrice(any());
    doNothing().when(feignHandler).validatePurchaseDetails(any());
    KakaopayReadyResponseDto mockResponseDto = createKakaopayReadyResponseDto();
    when(feignHandler.ready(any())).thenReturn(mockResponseDto);

    assertThatThrownBy(
            () ->
                orderFacade.readyForOrder(userId, request, OrderType.DELIVERY, OrderMethod.DIRECT))
        .isInstanceOf(InvalidOrderAmountException.class)
        .hasMessage("유효하지 않은 주문 금액입니다");
  }

  @Test
  @DisplayName("픽업 주문 - 준비단계 ")
  void readyForPickupOrder() {
    doNothing().when(feignHandler).validatePrice(any());
    doNothing().when(feignHandler).validatePurchaseDetails(any());
    KakaopayReadyResponseDto mockResponseDto = createKakaopayReadyResponseDto();
    when(feignHandler.ready(any())).thenReturn(mockResponseDto);

    KakaopayReadyResponseDto responseDto =
        orderFacade.readyForPickupOrder(1L, createOrderForPickupRequest(), OrderType.PICKUP);

    assertNotNull(responseDto);
  }

  @Test
  @DisplayName("구독 주문 - 준비단계")
  void readyForSubscriptionOrder() {
    Long userId = 1L;

    OrderForSubscriptionDto request = createOrderForSubscriptionRequest();

    doNothing().when(feignHandler).validatePrice(any());
    doNothing().when(feignHandler).validatePurchaseDetails(any());
    KakaopayReadyResponseDto mockResponseDto = createKakaopayReadyResponseDto();
    when(feignHandler.ready(any())).thenReturn(mockResponseDto);

    KakaopayReadyResponseDto responseDto =
        orderFacade.readyForSubscriptionOrder(userId, request, OrderType.SUBSCRIBE);

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
    redisOperation.saveIntoRedis(orderId, orderInfo);

    orderFacade.requestOrder(orderId, orderType, pgToken);
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
    redisOperation.saveIntoRedis(orderId, pickupOrderInfo);

    orderFacade.requestOrder(orderId, orderType, pgToken);
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
    redisOperation.saveIntoRedis(orderId, subscriptionOrderInfo);

    orderFacade.requestOrder(orderId, orderType, pgToken);
    processOrderDtoKafkaProducer = mock(KafkaProducer.class);

    doNothing()
        .when(processOrderDtoKafkaProducer)
        .send(eq("coupon-use"), any(ProcessOrderDto.class));
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
}
