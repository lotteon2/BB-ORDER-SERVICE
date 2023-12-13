package kr.bb.order.service;

import java.util.List;
import java.util.stream.Collectors;
import kr.bb.order.dto.request.payment.PaymentInfoDto;
import kr.bb.order.dto.request.product.ProductInfoDto;
import kr.bb.order.dto.response.order.OrderDeliveryGroupDto;
import kr.bb.order.dto.response.order.OrderDeliveryPageInfoDto;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.entity.delivery.OrderDeliveryStatus;
import kr.bb.order.entity.delivery.OrderGroup;
import kr.bb.order.feign.PaymentServiceClient;
import kr.bb.order.feign.ProductServiceClient;
import kr.bb.order.repository.OrderDeliveryRepository;
import kr.bb.order.repository.OrderGroupRepository;
import kr.bb.order.repository.OrderProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class OrderListService {
  private final OrderGroupRepository orderGroupRepository;
  private final OrderDeliveryRepository orderDeliveryRepository;
  private final PaymentServiceClient paymentServiceClient;
  private final ProductServiceClient productServiceClient;
  private final OrderProductRepository orderProductRepository;

  //  public OrderDeliveryPageInfoForSeller getOrderDeliveryListForSeller(@RequestHeader Long
  // userId, Pageable pageable, String status, Long storeId){
  //    orderPickupRepository.findByStoreIdSortedByCreatedAtDesc(userId, pageable);
  //
  //  }

  public OrderDeliveryPageInfoDto getUserOrderDeliveryList(
      Long userId, Pageable pageable, OrderDeliveryStatus orderDeliveryStatus) {
    // pageable만큼의 orderGroup을 최신 날짜순으로 가져온다.
    Page<OrderGroup> orderGroupsPerPage =
        orderGroupRepository.findByUserIdAndOrderDeliveryStatusSortedByCreatedAtDesc(
            userId, pageable, orderDeliveryStatus);
    List<OrderGroup> orderGroupsList = orderGroupsPerPage.getContent();

    Long totalCnt = (long) orderGroupsPerPage.getTotalPages();
    List<Long> storeCounts =
        orderGroupsList.stream()
            .map(orderGroup -> (long) orderGroup.getOrderDeliveryList().size())
            .collect(Collectors.toList());

    List<String> productIds = getProductIds(orderGroupsList);
    List<ProductInfoDto> productInfo = productServiceClient.getProductInfo(productIds).getData();

    List<String> orderGroupIds = getOrderGroupIds(orderGroupsList);
    List<PaymentInfoDto> paymentInfo = paymentServiceClient.getPaymentInfo(orderGroupIds).getData();

    List<OrderDeliveryGroupDto> orderDeliveryGroupDtos =
        OrderDeliveryGroupDto.toDto(orderGroupsList, storeCounts, productInfo, paymentInfo);

    return OrderDeliveryPageInfoDto.toDto(totalCnt, orderDeliveryGroupDtos);
  }

  // 각 주문그룹id의 첫번째 상품id만 추출하기
  List<String> getProductIds(List<OrderGroup> orderGroupsList) {
    return orderGroupsList.stream()
            .flatMap(orderGroup -> orderGroup.getOrderDeliveryList().stream())
            .map(OrderDelivery::getOrderDeliveryProducts)
            .map(orderDeliveryProducts -> orderDeliveryProducts.get(0).getProductId())
            .collect(Collectors.toList());
  }

  List<String> getOrderGroupIds(List<OrderGroup> orderGroupList) {
    return orderGroupList.stream().map(OrderGroup::getOrderGroupId).collect(Collectors.toList());
  }
}
