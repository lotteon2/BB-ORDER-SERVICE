package kr.bb.order.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import bloomingblooms.response.CommonResponse;
import java.util.ArrayList;
import java.util.List;
import kr.bb.order.dto.request.orderForDelivery.OrderForDeliveryRequest;
import kr.bb.order.dto.request.orderForDelivery.OrderInfoByStore;
import kr.bb.order.dto.request.orderForDelivery.ProductCreate;
import kr.bb.order.dto.response.payment.KakaopayReadyResponseDto;
import kr.bb.order.entity.redis.OrderInfo;
import kr.bb.order.exception.InvalidOrderAmountException;
import kr.bb.order.feign.PaymentServiceClient;
import kr.bb.order.feign.ProductServiceClient;
import kr.bb.order.feign.StoreServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@Transactional
class OrderServiceTest extends AbstractContainerBaseTest {
  @Autowired private OrderService orderService;
  @MockBean private ProductServiceClient productServiceClient;
  @MockBean private StoreServiceClient storeServiceClient;
  @MockBean private PaymentServiceClient paymentServiceClient;
  @Autowired private RedisTemplate<String, OrderInfo> redisTemplate;
  @Autowired private OrderManager orderManager;

  @BeforeEach
  void setup() {
    orderService =
        new OrderService(
            productServiceClient,
            storeServiceClient,
            paymentServiceClient,
            redisTemplate,
            orderManager);
  }

  @Test
  @DisplayName("바로 주문하기")
  public void createDirectOrder() {
    Long userId = 1L;
    Long sumOfActualAmount = 90500L;
    OrderForDeliveryRequest request = createOrderForDeliveryRequest(sumOfActualAmount);

    Mockito.when(productServiceClient.validatePrice(any()))
        .thenReturn(CommonResponse.success(null));
    Mockito.when(storeServiceClient.validatePurchaseDetails(any()))
        .thenReturn(CommonResponse.success(null));
    KakaopayReadyResponseDto mockResponseDto = createKakaopayReadyResponseDto();
    Mockito.when(paymentServiceClient.ready(any()))
        .thenReturn(CommonResponse.success(mockResponseDto));

    KakaopayReadyResponseDto responseDto = orderService.receiveOrderForDelivery(userId, request);

    assertNotNull(responseDto);
  }

  @Test
  @DisplayName("잘못된 결제금액으로는 주문이 불가능하다")
  public void orderCannotBeMadeToWrongAmount() {
    Long userId = 1L;
    Long wrongAmount = 25000L;
    OrderForDeliveryRequest request = createOrderForDeliveryRequest(wrongAmount);

    Mockito.when(productServiceClient.validatePrice(any()))
        .thenReturn(CommonResponse.success(null));
    Mockito.when(storeServiceClient.validatePurchaseDetails(any()))
        .thenReturn(CommonResponse.success(null));
    KakaopayReadyResponseDto mockResponseDto = createKakaopayReadyResponseDto();
    Mockito.when(paymentServiceClient.ready(any()))
        .thenReturn(CommonResponse.success(mockResponseDto));

    assertThatThrownBy(() -> orderService.receiveOrderForDelivery(userId, request))
        .isInstanceOf(InvalidOrderAmountException.class)
        .hasMessage("유효하지 않은 주문 금액입니다");
  }

  public OrderForDeliveryRequest createOrderForDeliveryRequest(Long sumOfActualAmount) {
    return OrderForDeliveryRequest.builder()
        .orderInfoByStores(createOrderInfoByStores())
        .sumOfActualAmount(sumOfActualAmount)
        .isSubscriptionPay(false)
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
            .productId(1L)
            .productName("상품명")
            .quantity(2L)
            .price(35000L)
            .build());
    list.add(
        ProductCreate.builder()
            .productId(2L)
            .productName("상품명")
            .quantity(1L)
            .price(20000L)
            .build());
    return list;
  }

  public KakaopayReadyResponseDto createKakaopayReadyResponseDto() {
    return KakaopayReadyResponseDto.builder()
        .nextRedirectPcUrl("임시redirectUrl주소")
        .tid("임시tid코드")
        .build();
  }
}
