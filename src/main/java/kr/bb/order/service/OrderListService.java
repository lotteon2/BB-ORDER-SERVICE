package kr.bb.order.service;

import bloomingblooms.domain.delivery.DeliveryInfoDto;
import bloomingblooms.domain.notification.delivery.DeliveryStatus;
import bloomingblooms.domain.payment.PaymentInfoDto;
import bloomingblooms.domain.product.ProductInformation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kr.bb.order.dto.request.PaymentInfoMapDto;
import kr.bb.order.dto.response.order.list.OrderDeliveryDetailsForSeller;
import kr.bb.order.dto.response.order.list.OrderDeliveryGroupDto;
import kr.bb.order.dto.response.order.list.OrderDeliveryInfoForSeller;
import kr.bb.order.dto.response.order.list.OrderDeliveryPageInfoDto;
import kr.bb.order.dto.response.order.list.OrderDeliveryPageInfoForSeller;
import kr.bb.order.entity.OrderDeliveryProduct;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.entity.delivery.OrderGroup;
import kr.bb.order.feign.DeliveryServiceClient;
import kr.bb.order.feign.PaymentServiceClient;
import kr.bb.order.feign.ProductServiceClient;
import kr.bb.order.repository.OrderDeliveryRepository;
import kr.bb.order.repository.OrderGroupRepository;
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
  private final DeliveryServiceClient deliveryServiceClient;

  public OrderDeliveryPageInfoDto getUserOrderDeliveryList(
      Long userId, Pageable pageable, DeliveryStatus deliveryStatus) {
    // pageable만큼의 orderGroup을 최신 날짜순으로 가져온다.
    Page<OrderGroup> orderGroupsPerPage =
        orderGroupRepository.findByUserIdAndOrderDeliveryStatusSortedByCreatedAtDesc(
            userId, pageable, deliveryStatus);
    List<OrderGroup> orderGroupsList = orderGroupsPerPage.getContent();

    Long totalCnt = (long) orderGroupsList.size();
    // 각 value: 주문그룹별 가게 수
    List<Long> storeCounts =
        orderGroupsList.stream()
            .map(orderGroup -> (long) orderGroup.getOrderDeliveryList().size())
            .collect(Collectors.toList());

    // 주문그룹별 대표 상품id
    List<String> productIds = getProductIds(orderGroupsList);
    List<ProductInformation> productInfo = productServiceClient.getProductInfo(productIds).getData();
    Map<String, ProductInformation> productInfoDtoMap = productInfo.stream().collect(Collectors.toMap(
            ProductInformation::getProductId, productInfoDto -> productInfoDto));

    List<String> orderGroupIds = getOrderGroupIds(orderGroupsList);
    PaymentInfoMapDto paymentInfoMapDto = paymentServiceClient.getPaymentInfo(orderGroupIds).getData();
    Map<String, PaymentInfoDto> paymentInfoDtoMap = paymentInfoMapDto.getPaymentInfoDtoMap();

    List<OrderDeliveryGroupDto> orderDeliveryGroupDtos =
        OrderDeliveryGroupDto.toDto(orderGroupsList, storeCounts, productIds, productInfoDtoMap, paymentInfoDtoMap);

    return OrderDeliveryPageInfoDto.toDto(totalCnt, orderDeliveryGroupDtos);
  }

  public OrderDeliveryPageInfoForSeller getOrderDeliveryListForSeller(
          Pageable pageable, DeliveryStatus status, Long storeId) {
    Page<OrderDelivery> orderDeliveriesPerPage =
        orderDeliveryRepository.findByStoreIdSortedByCreatedAtDesc(storeId, pageable, status);

    Long totalCnt = (long) orderDeliveriesPerPage.getContent().size();

    List<String> orderGroupIds =
        orderDeliveriesPerPage.getContent().stream()
            .map(orderDelivery -> orderDelivery.getOrderGroup().getOrderGroupId())
            .collect(Collectors.toList());

    // product, payment, delivery-service feign 요청
    List<String> productIds = orderDeliveriesPerPage.getContent().stream().flatMap(orderDelivery -> orderDelivery.getOrderDeliveryProducts().stream()).map(OrderDeliveryProduct::getProductId).collect(
            Collectors.toList());
    List<ProductInformation> productInformation = productServiceClient.getProductInfo(productIds).getData();
    Map<String, ProductInformation> productIdMap = productInformation.stream()
            .collect(Collectors.toMap(ProductInformation::getProductId, dto -> dto));

    PaymentInfoMapDto paymentInfoMapDto = paymentServiceClient.getPaymentInfo(orderGroupIds).getData();
    Map<String, PaymentInfoDto> paymentInfoDtoMap = paymentInfoMapDto.getPaymentInfoDtoMap();

    List<Long> deliveryIds = orderDeliveriesPerPage.stream().map(OrderDelivery::getDeliveryId).collect(
            Collectors.toList());
    Map<Long, DeliveryInfoDto> deliveryInfoMap = deliveryServiceClient.getDeliveryInfo(
            deliveryIds).getData();


    List<OrderDeliveryInfoForSeller> infoDtoList = new ArrayList<>();
    for(OrderDelivery orderDelivery : orderDeliveriesPerPage.getContent()){
      List<OrderDeliveryDetailsForSeller> detailsDtoList = new ArrayList<>();
      for(OrderDeliveryProduct orderDeliveryProduct : orderDelivery.getOrderDeliveryProducts()){
        OrderDeliveryDetailsForSeller details = OrderDeliveryDetailsForSeller.toDto(orderDeliveryProduct, productIdMap);
        detailsDtoList.add(details);
      }
      OrderDeliveryInfoForSeller infoDto = OrderDeliveryInfoForSeller.toDto(orderDelivery, detailsDtoList,
              paymentInfoDtoMap, deliveryInfoMap);
      infoDtoList.add(infoDto);
    }
    return OrderDeliveryPageInfoForSeller.toDto(totalCnt, infoDtoList);
  }

  // 각 주문그룹id의 '첫번째' 상품id만 추출하기
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
