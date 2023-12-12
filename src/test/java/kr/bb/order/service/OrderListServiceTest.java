package kr.bb.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import bloomingblooms.response.CommonResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import kr.bb.order.dto.request.payment.PaymentInfoDto;
import kr.bb.order.dto.request.product.ProductInfoDto;
import kr.bb.order.dto.response.order.OrderDeliveryPageInfoDto;
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

  @Test
  @DisplayName("주문 목록 조회")
  public void userCanReadHisOrderDeliveryList() {
    Long userId = 1L;
    String orderGroupId = "그룹주문id";
    OrderGroup orderGroup = OrderGroup.builder().orderGroupId(orderGroupId).userId(userId).build();
    OrderGroup savedOrderGroup = orderGroupRepository.save(orderGroup);

    Pageable pageable = PageRequest.of(0, 5);

    orderDeliveryRepository.save(createOrderDelivery(savedOrderGroup));

    List<ProductInfoDto> productInfoDtos = createProductInfoList();
    when(productServiceClient.getProductInfo(any()))
        .thenReturn(CommonResponse.success(productInfoDtos));

    List<PaymentInfoDto> paymentInfoDtos = createPaymentInfoList(orderGroupId);
    when(paymentServiceClient.getPaymentInfo(any()))
        .thenReturn(CommonResponse.success(paymentInfoDtos));

    OrderDeliveryPageInfoDto orderDeliveryPageInfoDto =
        orderListService.getOrderDeliveryListForUser(userId, pageable, OrderDeliveryStatus.PENDING);

    assertThat(orderDeliveryPageInfoDto.getTotalCnt().equals(1L)).isTrue();
    assertThat(orderDeliveryPageInfoDto.getOrders().get(0).getOrderGroupId().equals("그룹주문id")).isTrue();
  }

  OrderDelivery createOrderDelivery(OrderGroup orderGroup) {
    orderGroupRepository.findById(orderGroup.getOrderGroupId());
    return OrderDelivery.builder()
            .orderGroup(orderGroup)
        .orderDeliveryId("가게주문id")
        .storeId(1L)
        .deliveryId(1L)
        .orderDeliveryTotalAmount(39800L)
        .orderDeliveryCouponAmount(0L)
        .build();
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
            .orderId(orderGroupId)
            .paymentActualAmount(39800L)
            .createdAt(LocalDateTime.now())
            .build());

    return paymentInfoDtos;
  }
}
