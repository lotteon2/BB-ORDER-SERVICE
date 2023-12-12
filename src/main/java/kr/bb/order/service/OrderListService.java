package kr.bb.order.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import kr.bb.order.dto.request.payment.PaymentInfoDto;
import kr.bb.order.dto.request.product.ProductInfoDto;
import kr.bb.order.dto.response.order.OrderDeliveryDetailsDto;
import kr.bb.order.dto.response.order.OrderDeliveryGroupInfoDto;
import kr.bb.order.dto.response.order.OrderDeliveryInfoDto;
import kr.bb.order.dto.response.order.OrderDeliveryPageInfoDto;
import kr.bb.order.entity.OrderDeliveryProduct;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.entity.delivery.OrderDeliveryStatus;
import kr.bb.order.entity.delivery.OrderGroup;
import kr.bb.order.exception.FeignClientException;
import kr.bb.order.exception.common.ErrorCode;
import kr.bb.order.feign.PaymentServiceClient;
import kr.bb.order.feign.ProductServiceClient;
import kr.bb.order.repository.OrderDeliveryRepository;
import kr.bb.order.repository.OrderGroupRepository;
import kr.bb.order.repository.OrderPickupRepository;
import kr.bb.order.repository.OrderProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderListService {
  private final OrderGroupRepository orderGroupRepository;
  private final OrderDeliveryRepository orderDeliveryRepository;
  private final PaymentServiceClient paymentServiceClient;
  private final ProductServiceClient productServiceClient;
  private final OrderProductRepository orderProductRepository;
  private final OrderPickupRepository orderPickupRepository;

  @Transactional
  public OrderDeliveryPageInfoDto getOrderDeliveryListForUser(Long userId, Pageable pageable, OrderDeliveryStatus orderDeliveryStatus) {
    // pageable만큼의 orderGroup을 최신 날짜순으로 가져온다.
    Page<OrderGroup> orderGroupsPerPage =
        orderGroupRepository.findByUserIdAndOrderDeliveryStatusSortedByCreatedAtDesc(userId, pageable, orderDeliveryStatus);
    List<OrderGroup> orderGroupsList = orderGroupsPerPage.getContent();

    Long totalCnt = (long) orderGroupsPerPage.getTotalPages();

    // 그룹에 속한 주문정보를 다 찾기.
    List<OrderDelivery> allByOrderGroups =
        orderDeliveryRepository.findAllByOrderGroups(orderGroupsList);
    // 그룹id-List<OrderDelivery> 맵 만들기.
    Map<OrderGroup, List<OrderDelivery>> orderDeliveryMap =
        allByOrderGroups.stream().collect(Collectors.groupingBy(OrderDelivery::getOrderGroup));

    // 주문id 목록 만들기. (주문상품 찾아내는 용도)
    List<String> orderIds =
        orderDeliveryMap.values().stream()
            .flatMap(List::stream)
            .map(OrderDelivery::getOrderDeliveryId)
            .collect(Collectors.toList());
    // 주문그룹id 목록 만들기. (feign 통신 용도)
    List<String> orderGroupIds =
        orderGroupsList.stream().map(OrderGroup::getOrderGroupId).collect(Collectors.toList());

    // List<OrderDelivery> 에 속한 모든 List<OrderProduct> 찾기.
    List<OrderDeliveryProduct> orderDeliveryProducts = orderProductRepository.findAllByOrderIds(orderIds);
    List<String> productIds =
        orderDeliveryProducts.stream().map(OrderDeliveryProduct::getProductId).collect(Collectors.toList());

    // product-service로 정보 요청 (각 상품의 정보를 받아온다)
    List<ProductInfoDto> productInfoDtos =
        productServiceClient.getProductInfo(productIds).getData();

    // payment-service로 정보 요청 ( 그룹주문id 목록에 해당하는 결제정보 목록을 받아온다)
    List<PaymentInfoDto> paymentInfoDtos =
        paymentServiceClient.getPaymentInfo(orderGroupIds).getData();

    // 1차 wrapping
    // 가게주문id에 속한 상품 목록 정보(List<OrderProduct>)로 OrderDeliveryDetails를 생성한다.
    // 2차 wrapping
    // 그리고 그 OrderDeliveryDetails로 OrderDeliveryInfo를 생성한다.
    // 3차 wrapping
    // OrderDeliveryInfo로 OrderDeliveryGroupInfo를 생성한다.
    List<OrderDeliveryGroupInfoDto> orderDeliveryGroupInfoDtos = new ArrayList<>();
    for (OrderGroup orderGroup : orderDeliveryMap.keySet()) {
      List<OrderDelivery> orderDeliveries = orderDeliveryMap.get(orderGroup);
      List<OrderDeliveryInfoDto> orderDeliveryInfoDtos = new ArrayList<>();
      for (OrderDelivery orderDelivery : orderDeliveries) {
        // 가게별 주문 찾기
        String orderDeliveryId = orderDelivery.getOrderDeliveryId();
        // 주문상품 정보 찾기
        List<OrderDeliveryProduct> filteredOrderDeliveryProducts =
            orderDeliveryProducts.stream()
                .filter(
                    orderProduct -> Objects.equals(orderProduct.getOrderId(), orderDeliveryId)).collect(
                            Collectors.toList());
        // 1차 wrapping
        List<OrderDeliveryDetailsDto> orderDeliveryDetailDtos = OrderDeliveryDetailsDto.toDto(productIds, productInfoDtos,
                filteredOrderDeliveryProducts);
        // 2차 wrapping
        OrderDeliveryInfoDto orderDeliveryInfoDto =
                OrderDeliveryInfoDto.toDto(orderDeliveryId, orderDeliveries, orderDeliveryProducts,
                        orderDeliveryDetailDtos);
        orderDeliveryInfoDtos.add(orderDeliveryInfoDto);
      }

      PaymentInfoDto paymentInfoDto = paymentInfoDtos.stream()
              .filter(eachPaymentInfoDto -> eachPaymentInfoDto.getOrderId().equals(orderGroup.getOrderGroupId()))
              .findFirst().orElseThrow(()->
                      new FeignClientException(ErrorCode.PAYMENT_FEIGN_CLIENT_EXCEPTION.getMessage()));
      OrderDeliveryGroupInfoDto orderDeliveryGroupInfoDto = OrderDeliveryGroupInfoDto.toDto(orderGroup.getOrderGroupId(),
              orderDeliveryInfoDtos,
              paymentInfoDto);

      orderDeliveryGroupInfoDtos.add(orderDeliveryGroupInfoDto);
    }

    return OrderDeliveryPageInfoDto.builder()
            .totalCnt(totalCnt)
            .orders(orderDeliveryGroupInfoDtos)
            .build();
  }

//  public OrderDeliveryPageInfoForSeller getOrderDeliveryListForSeller(@RequestHeader Long userId, Pageable pageable){
//    orderPickupRepository.findByStoreIdSortedByCreatedAtDesc(userId, pageable);
//
//  }


}
