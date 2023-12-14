package kr.bb.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import bloomingblooms.response.CommonResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import kr.bb.order.dto.request.payment.PaymentInfoDto;
import kr.bb.order.dto.request.product.ProductInfoDto;
import kr.bb.order.dto.response.delivery.DeliveryInfoDto;
import kr.bb.order.dto.response.order.OrderDeliveryPageInfoDto;
import kr.bb.order.dto.response.order.OrderDeliveryPageInfoForSeller;
import kr.bb.order.entity.OrderDeliveryProduct;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.entity.delivery.OrderDeliveryStatus;
import kr.bb.order.entity.delivery.OrderGroup;
import kr.bb.order.feign.DeliveryServiceClient;
import kr.bb.order.feign.PaymentServiceClient;
import kr.bb.order.feign.ProductServiceClient;
import kr.bb.order.repository.OrderDeliveryRepository;
import kr.bb.order.repository.OrderGroupRepository;
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
  @Autowired private OrderDeliveryRepository orderDeliveryRepository;
  @Autowired private OrderGroupRepository orderGroupRepository;
  @MockBean private PaymentServiceClient paymentServiceClient;
  @MockBean private ProductServiceClient productServiceClient;
  @MockBean private DeliveryServiceClient deliveryServiceClient;
  @Autowired private EntityManager em;

  @Test
  @DisplayName("주문 목록 조회 - 회원")
  public void userCanReadHisOrderDeliveryList() {
    Long userId = 1L;
    String orderGroupId = "그룹주문id";

    Pageable pageable = PageRequest.of(0, 5);

    List<ProductInfoDto> productInfoDtos = createProductInfoList();
    when(productServiceClient.getProductInfo(any()))
        .thenReturn(CommonResponse.success(productInfoDtos));

    List<PaymentInfoDto> paymentInfoDtos = createPaymentInfoList(orderGroupId);
    when(paymentServiceClient.getPaymentInfo(any()))
        .thenReturn(CommonResponse.success(paymentInfoDtos));

    OrderDeliveryPageInfoDto orderDeliveryPageInfoDto =
        orderListService.getUserOrderDeliveryList(userId, pageable, OrderDeliveryStatus.PENDING);

    assertThat(orderDeliveryPageInfoDto.getTotalCnt().equals(1L)).isTrue();
    assertThat(orderDeliveryPageInfoDto.getOrders().get(0).getKey().equals("그룹주문id")).isTrue();
  }

  @Test
  @DisplayName("주문 목록 조회 - 가게")
  public void sellerCanReadHisOrderDeliveryList(){
    Pageable pageable = PageRequest.of(0, 2);
    OrderDeliveryStatus status = OrderDeliveryStatus.PENDING;
    Long storeId = 1L;
    String groupId = "그룹주문id";

    List<ProductInfoDto> productInfoDtos = createProductInfoList();
    when(productServiceClient.getProductInfo(any()))
            .thenReturn(CommonResponse.success(productInfoDtos));

    List<PaymentInfoDto> paymentInfoDtos = createPaymentInfoList(groupId);
    when(paymentServiceClient.getPaymentInfo(any()))
            .thenReturn(CommonResponse.success(paymentInfoDtos));

    Map<Long, DeliveryInfoDto> deliveryInfoMap = createDeliveryInfoMap();
    when(deliveryServiceClient.getDeliveryInfo(
            any())).thenReturn(CommonResponse.success((deliveryInfoMap)));

    OrderDeliveryPageInfoForSeller infoForSeller = orderListService.getOrderDeliveryListForSeller(pageable, status, storeId);

    assertThat(infoForSeller.getOrders().size()).isEqualTo(1);
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

  List<PaymentInfoDto> createPaymentInfoList(String orderGroupId) {
    List<PaymentInfoDto> paymentInfoDtos = new ArrayList<>();
    paymentInfoDtos.add(
        PaymentInfoDto.builder()
            .orderGroupId(orderGroupId)
            .paymentActualAmount(39800L)
            .createdAt(LocalDateTime.now())
            .build());

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
