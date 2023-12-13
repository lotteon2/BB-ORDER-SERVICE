package kr.bb.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import bloomingblooms.response.CommonResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import kr.bb.order.dto.request.payment.PaymentInfoDto;
import kr.bb.order.dto.request.product.ProductInfoDto;
import kr.bb.order.dto.response.order.OrderDeliveryGroupDto;
import kr.bb.order.dto.response.order.OrderDeliveryPageInfoDto;
import kr.bb.order.entity.OrderDeliveryProduct;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.entity.delivery.OrderDeliveryStatus;
import kr.bb.order.entity.delivery.OrderGroup;
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
  @Autowired private EntityManager em;

  @Test
  @DisplayName("주문 목록 조회")
  public void userCanReadHisOrderDeliveryList() {
    Long userId = 1L;
    String orderGroupId = "그룹주문id";
    // 부모 영속화
    OrderGroup orderGroup = OrderGroup.builder().orderGroupId(orderGroupId).userId(userId).build();
    em.persist(orderGroup);

    // 자식 생성, 자식-부모 set, 자식 영속화
    List<OrderDelivery> orderDeliveryList = createOrderDelivery();
    for (OrderDelivery orderDelivery : orderDeliveryList) {
      orderDelivery.setOrderGroup(orderGroup);
      em.persist(orderDelivery);
    }
    // 부모-자식 set
    orderGroup.setOrderDeliveryList(orderDeliveryList);

    // 자식 생성, 자식-부모 set, 자식 영속화
    createOrderDeliveryProduct(orderDeliveryList);

    for (OrderDelivery orderDelivery : orderDeliveryList) {
      for (OrderDeliveryProduct orderDeliveryProduct : orderDelivery.getOrderDeliveryProducts()) {
        orderDeliveryProduct.setOrderDelivery(orderDelivery);
        em.persist(orderDeliveryProduct);
      }
      orderDelivery.setOrderDeliveryProduct(orderDelivery.getOrderDeliveryProducts());
    }

    em.flush();

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

  List<OrderDelivery> createOrderDelivery() {
    List<OrderDelivery> orderDeliveryList = new ArrayList<>();

    OrderDelivery orderDelivery =
        OrderDelivery.builder()
            .orderDeliveryId("가게주문id")
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
}
