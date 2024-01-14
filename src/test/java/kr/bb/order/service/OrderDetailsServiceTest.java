package kr.bb.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import bloomingblooms.domain.delivery.DeliveryInfoDto;
import bloomingblooms.domain.product.ProductInformation;
import bloomingblooms.response.CommonResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityExistsException;
import kr.bb.order.dto.ProductStatusChangeDto;
import kr.bb.order.dto.response.order.WeeklySalesInfoDto;
import kr.bb.order.dto.response.order.details.OrderDeliveryGroup;
import kr.bb.order.dto.response.order.details.OrderInfoForStoreForSeller;
import kr.bb.order.entity.OrderDeliveryProduct;
import kr.bb.order.feign.DeliveryServiceClient;
import kr.bb.order.feign.PaymentServiceClient;
import kr.bb.order.feign.ProductServiceClient;
import kr.bb.order.feign.StoreServiceClient;
import kr.bb.order.repository.OrderDeliveryProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class OrderDetailsServiceTest {
  @Autowired private OrderDetailsService orderDetailsService;
  @MockBean private ProductServiceClient productServiceClient;
  @MockBean private StoreServiceClient storeServiceClient;
  @MockBean private DeliveryServiceClient deliveryServiceClient;
  @MockBean private PaymentServiceClient paymentServiceClient;
  @MockBean private SimpleMessageListenerContainer simpleMessageListenerContainer;
  @Autowired private OrderSqsService orderSqsService;
  @Autowired private OrderDeliveryProductRepository orderDeliveryProductRepository;

  @Test
  @DisplayName("주문 상세 조회 - 회원")
  void getOrderDetailsForUser() {
    String orderGroupId = "그룹주문id";

    List<ProductInformation> productInformations = createProductInfoList();
    when(productServiceClient.getProductInfo(any()))
        .thenReturn(CommonResponse.success(productInformations));

    Map<Long, String> storeNameMap = createStoreNameMap();
    when(storeServiceClient.getStoreName(any())).thenReturn(CommonResponse.success(storeNameMap));

    Map<Long, DeliveryInfoDto> deliveryInfoMap = createDeliveryInfoMap();
    when(deliveryServiceClient.getDeliveryInfo(any()))
        .thenReturn(CommonResponse.success(deliveryInfoMap));

    String paymentDate = createPaymentDate();
    when(paymentServiceClient.getPaymentDate(any()))
        .thenReturn(CommonResponse.success(paymentDate));

    OrderDeliveryGroup orderDeliveryGroup =
        orderDetailsService.getOrderDetailsForUser(orderGroupId);

    assertThat(orderDeliveryGroup.getOrdererName().equals("주문자 이름")).isTrue();
  }

  private String createPaymentDate() {
    return LocalDate.now().toString();
  }

  private Map<Long, DeliveryInfoDto> createDeliveryInfoMap() {
    Map<Long, DeliveryInfoDto> map = new HashMap<>();
    DeliveryInfoDto deliveryInfoDto =
        DeliveryInfoDto.builder()
            .ordererName("주문자 이름")
            .ordererPhone("주문자 전화번호")
            .ordererEmail("주문자 이메일")
            .recipientName("수신자 이름")
            .recipientPhone("수신자 전화번호")
            .zipcode("우편번호")
            .roadName("도로명주소")
            .addressDetail("상세주소")
            .deliveryRequest("배송요청")
            .deliveryCost(0L)
            .build();
    map.put(1L, deliveryInfoDto);
    return map;
  }

  private Map<Long, String> createStoreNameMap() {
    Map<Long, String> map = new HashMap<>();
    map.put(1L, "가게이름");
    return map;
  }

  List<ProductInformation> createProductInfoList() {
    List<ProductInformation> productInformations = new ArrayList<>();
    productInformations.add(
        ProductInformation.builder()
            .productId("꽃id-1")
            .productName("꽃이름-1")
            .productThumbnail("썸네일url-1")
            .build());
    productInformations.add(
        ProductInformation.builder()
            .productId("꽃id-2")
            .productName("꽃이름-2")
            .productThumbnail("썸네일url-2")
            .build());
    return productInformations;
  }

  @Test
  @DisplayName("주문 상세 조회 - 가게")
  void getOrderDetailsForSeller() {
    String orderDeliveryId = "가게주문id";

    List<ProductInformation> productInformations = createProductInfoList();
    when(productServiceClient.getProductInfo(any()))
        .thenReturn(CommonResponse.success(productInformations));

    Map<Long, String> storeNameMap = createStoreNameMap();
    when(storeServiceClient.getStoreName(any())).thenReturn(CommonResponse.success(storeNameMap));

    Map<Long, DeliveryInfoDto> deliveryInfoMap = createDeliveryInfoMap();
    when(deliveryServiceClient.getDeliveryInfo(any()))
        .thenReturn(CommonResponse.success(deliveryInfoMap));

    String paymentDate = createPaymentDate();
    when(paymentServiceClient.getPaymentDate(any()))
        .thenReturn(CommonResponse.success(paymentDate));

    OrderInfoForStoreForSeller orderDetailsForSeller =
        orderDetailsService.getOrderDetailsForSeller(orderDeliveryId);

    assertThat(orderDetailsForSeller.getOrdererName().equals("주문자 이름")).isTrue();
  }

  @Test
  @DisplayName("주간별 가게 매출 조회")
  void getWeeklySalesInfo() {
    Long storeId = 1L;
    WeeklySalesInfoDto weeklySalesInfo = orderDetailsService.getWeeklySalesInfo(storeId);

    assertThat(weeklySalesInfo)
        .extracting("categories", "data")
        .contains(
            Arrays.asList(
                (LocalDate.now().minusDays(4).toString()),
                (LocalDate.now().minusDays(3).toString()),
                (LocalDate.now().minusDays(2).toString()),
                (LocalDate.now().minusDays(1).toString())),
            Arrays.asList(49800L, 49800L, 39800L, 39800L));
  }

  @Test
  @DisplayName("Feign 요청으로 인한 배송id 반환하기")
  void getDeliveryId() {
    String orderDeliveryId = "가게주문id";
    Long deliveryId = orderDetailsService.getDeliveryId(orderDeliveryId);

    assertThat(deliveryId).isEqualTo(1L);
  }

  // 리뷰,카드 상태 변경 테스트
  @Test
  void updateReviewAndCardStatus() {
    ProductStatusChangeDto statusChangeDto = ProductStatusChangeDto.builder().id(1L).status("").build();

    orderSqsService.updateOrderDeliveryReview(statusChangeDto);
    orderSqsService.updateOrderDeliveryCard(statusChangeDto);

    OrderDeliveryProduct orderDeliveryProduct = orderDeliveryProductRepository.findById(
            statusChangeDto.getId()).orElseThrow(
            EntityExistsException::new);

    assertThat(orderDeliveryProduct.getCardStatus().toString()).isEqualTo("DONE");
    assertThat(orderDeliveryProduct.getReviewStatus().toString()).isEqualTo("DONE");
  }
}
