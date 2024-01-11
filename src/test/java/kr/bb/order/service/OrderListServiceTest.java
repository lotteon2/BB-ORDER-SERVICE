package kr.bb.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import bloomingblooms.domain.delivery.DeliveryInfoDto;
import bloomingblooms.domain.notification.delivery.DeliveryStatus;
import bloomingblooms.domain.payment.PaymentInfoDto;
import bloomingblooms.domain.product.ProductInformation;
import bloomingblooms.response.CommonResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kr.bb.order.dto.response.order.list.OrderDeliveryPageInfoDto;
import kr.bb.order.dto.response.order.list.OrderDeliveryPageInfoForSeller;
import kr.bb.order.entity.OrderDeliveryProduct;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.feign.DeliveryServiceClient;
import kr.bb.order.feign.PaymentServiceClient;
import kr.bb.order.feign.ProductServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class OrderListServiceTest {
  @Autowired private OrderListService orderListService;
  @MockBean private PaymentServiceClient paymentServiceClient;
  @MockBean private ProductServiceClient productServiceClient;
  @MockBean private DeliveryServiceClient deliveryServiceClient;

  @Test
  @DisplayName("주문 목록 조회 - 회원")
  public void userCanReadHisOrderDeliveryList() {
    Long userId = 1L;
    List<String> orderGroupIds = List.of("그룹주문id","그룹주문id2","그룹주문id3","그룹주문id4");

    Pageable pageable = PageRequest.of(0, 5);

    List<ProductInformation> productInformations = createProductInfoList();
    when(productServiceClient.getProductInfo(any()))
            .thenReturn(CommonResponse.success(productInformations));

    List<PaymentInfoDto> paymentInfoDtos = createPaymentInfoList(orderGroupIds);
    when(paymentServiceClient.getPaymentInfo(any()))
            .thenReturn(CommonResponse.success(paymentInfoDtos));

    OrderDeliveryPageInfoDto orderDeliveryPageInfoDto =
        orderListService.getUserOrderDeliveryList(userId, pageable, DeliveryStatus.PENDING);

    assertThat(orderDeliveryPageInfoDto.getTotalCnt()).isEqualTo(4L);
    assertThat(orderDeliveryPageInfoDto.getOrders().get(0).getKey().equals("그룹주문id4")).isTrue();
  }

  @Test
  @DisplayName("주문 목록 조회 - 가게")
  public void sellerCanReadHisOrderDeliveryList(){
    Pageable pageable = PageRequest.of(0, 5);
    DeliveryStatus status = DeliveryStatus.PENDING;
    Long storeId = 1L;
    List<String> groupIds = List.of("그룹주문id","그룹주문id2","그룹주문id3","그룹주문id4");

    List<ProductInformation> productInformations = createProductInfoList();
    when(productServiceClient.getProductInfo(any()))
            .thenReturn(CommonResponse.success(productInformations));

    List<PaymentInfoDto> paymentInfoDtos = createPaymentInfoList(groupIds);
    when(paymentServiceClient.getPaymentInfo(any()))
            .thenReturn(CommonResponse.success(paymentInfoDtos));

    Map<Long, DeliveryInfoDto> deliveryInfoMap = createDeliveryInfoMap();
    when(deliveryServiceClient.getDeliveryInfo(
            any())).thenReturn(CommonResponse.success((deliveryInfoMap)));

    OrderDeliveryPageInfoForSeller infoForSeller = orderListService.getOrderDeliveryListForSeller(pageable, status, storeId);

    assertThat(infoForSeller.getOrders().size()).isEqualTo(4);
  }


  List<OrderDelivery> createOrderDelivery() {
    List<OrderDelivery> orderDeliveryList = new ArrayList<>();

    OrderDelivery orderDelivery =
        OrderDelivery.builder()
            .orderDeliveryId("가게주문id2")
            .storeId(1L)
            .deliveryId(1L)
            .orderDeliveryTotalAmount(39800L)
            .orderDeliveryCouponAmount(0L)
            .build();

    orderDeliveryList.add(orderDelivery);
    return orderDeliveryList;
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
    productInformations.add(
            ProductInformation.builder()
                    .productId("꽃id-3")
                    .productName("꽃이름-3")
                    .productThumbnail("썸네일url-3")
                    .build());
    return productInformations;
  }

  List<PaymentInfoDto> createPaymentInfoList(List<String> orderGroupIds) {
    List<PaymentInfoDto> paymentInfoDtos = new ArrayList<>();
    for(String orderGroupId: orderGroupIds){
      paymentInfoDtos.add(
              PaymentInfoDto.builder()
                      .orderGroupId(orderGroupId)
                      .paymentActualAmount(39800L)
                      .createdAt(LocalDateTime.now())
                      .build());
    }

    return paymentInfoDtos;
  }

  void createOrderDeliveryProduct(List<OrderDelivery> orderDeliveryList) {
    List<OrderDeliveryProduct> orderDeliveryProducts = new ArrayList<>();
    for (OrderDelivery orderDelivery : orderDeliveryList) {
      OrderDeliveryProduct orderDeliveryProduct =
          OrderDeliveryProduct.builder()
              .orderDelivery(orderDelivery)
              .productId("꽃id-1")
              .orderProductPrice(39800L)
              .orderProductQuantity(1L)
              .build();

      orderDeliveryProducts.add(orderDeliveryProduct);
      orderDelivery.setOrderDeliveryProduct(orderDeliveryProducts);
    }
  }

  Map<Long, DeliveryInfoDto> createDeliveryInfoMap(){
    Map<Long, DeliveryInfoDto> map = new HashMap<>();
    DeliveryInfoDto deliveryInfoDto = DeliveryInfoDto.builder()
            .zipcode("우편번호")
            .roadName("도로명주소")
            .addressDetail("상세주소")
            .deliveryRequest("배송요청사항")
            .build();
    map.put(1L, deliveryInfoDto);
    return map;
  }
}
