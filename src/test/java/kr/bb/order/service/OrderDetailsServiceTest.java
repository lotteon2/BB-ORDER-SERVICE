package kr.bb.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import bloomingblooms.response.CommonResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kr.bb.order.dto.request.payment.PaymentInfoDto;
import kr.bb.order.dto.request.product.ProductInfoDto;
import kr.bb.order.dto.response.delivery.DeliveryInfoDto;
import kr.bb.order.dto.response.order.details.OrderDeliveryGroup;
import kr.bb.order.feign.DeliveryServiceClient;
import kr.bb.order.feign.PaymentServiceClient;
import kr.bb.order.feign.ProductServiceClient;
import kr.bb.order.feign.StoreServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class OrderDetailsServiceTest {
  @Autowired private OrderDetailsService orderDetailsService;
  @MockBean private ProductServiceClient productServiceClient;
  @MockBean private StoreServiceClient storeServiceClient;
  @MockBean private DeliveryServiceClient deliveryServiceClient;
  @MockBean private PaymentServiceClient paymentServiceClient;

  @Test
  @DisplayName("주문 상세 조회 - 회원")
  void getOrderDetailsForUser() {
    String orderGroupId = "그룹주문id";

    List<ProductInfoDto> productInfoDtos = createProductInfoList();
    when(productServiceClient.getProductInfo(any()))
        .thenReturn(CommonResponse.success(productInfoDtos));

    Map<Long, String> storeNameMap = createStoreNameMap();
    when(storeServiceClient.getStoreName(any())).thenReturn(CommonResponse.success(storeNameMap));

    Map<Long, DeliveryInfoDto> deliveryInfoMap = createDeliveryInfoMap();
    when(deliveryServiceClient.getDeliveryInfo(any()))
        .thenReturn(CommonResponse.success(deliveryInfoMap));

    String paymentDate = createPaymentDate();
    when(paymentServiceClient.getPaymentDate(any()))
        .thenReturn(CommonResponse.success(paymentDate));

    OrderDeliveryGroup orderDeliveryGroup = orderDetailsService.getOrderDetailsForUser(
            orderGroupId);

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

  List<ProductInfoDto> createProductInfoList() {
    List<ProductInfoDto> productInfoDtos = new ArrayList<>();
    productInfoDtos.add(
        ProductInfoDto.builder()
            .productId("꽃id-1")
            .productName("꽃이름-1")
            .productThumbnailImage("썸네일url-1")
            .build());
    return productInfoDtos;
  }

}
