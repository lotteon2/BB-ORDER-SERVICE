package kr.bb.order.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.EntityNotFoundException;
import kr.bb.order.dto.request.product.ProductInfoDto;
import kr.bb.order.dto.response.delivery.DeliveryInfoDto;
import kr.bb.order.dto.response.order.details.OrderDeliveryGroup;
import kr.bb.order.dto.response.order.details.OrderInfoForStore;
import kr.bb.order.dto.response.order.details.OrderInfoForStoreForSeller;
import kr.bb.order.dto.response.order.details.ProductRead;
import kr.bb.order.entity.OrderDeliveryProduct;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.feign.DeliveryServiceClient;
import kr.bb.order.feign.PaymentServiceClient;
import kr.bb.order.feign.ProductServiceClient;
import kr.bb.order.feign.StoreServiceClient;
import kr.bb.order.repository.OrderDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderDetailsService {
  private final OrderDeliveryRepository orderDeliveryRepository;
  private final ProductServiceClient productServiceClient;
  private final StoreServiceClient storeServiceClient;
  private final DeliveryServiceClient deliveryServiceClient;
  private final PaymentServiceClient paymentServiceClient;

  public OrderDeliveryGroup getOrderDetailsForUser(String orderGroupId) {
    List<OrderDelivery> orderDeliveryList =
        orderDeliveryRepository.findByOrderGroupId(orderGroupId);

    // feign 요청
    List<String> productIds =
        orderDeliveryList.stream()
            .flatMap(
                orderDelivery ->
                    orderDelivery.getOrderDeliveryProducts().stream()
                        .map(OrderDeliveryProduct::getProductId))
            .collect(Collectors.toList());
    List<ProductInfoDto> productInfoDtos =
        productServiceClient.getProductInfo(productIds).getData();
    Map<String, ProductInfoDto> productInfoDtoMap =
        productInfoDtos.stream()
            .collect(Collectors.toMap(ProductInfoDto::getProductId, dto -> dto));

    List<Long> storeIds =
        orderDeliveryList.stream().map(OrderDelivery::getStoreId).collect(Collectors.toList());
    Map<Long, String> storeNameMap = storeServiceClient.getStoreName(storeIds).getData();

    List<Long> deliveryIds =
        orderDeliveryList.stream().map(OrderDelivery::getDeliveryId).collect(Collectors.toList());
    Map<Long, DeliveryInfoDto> deliveryInfoMap =
        deliveryServiceClient.getDeliveryInfo(deliveryIds).getData();

    String paymentDate = paymentServiceClient.getPaymentDate(orderGroupId).getData();

    // DTO 데이터 wrapping
    List<OrderInfoForStore> orderInfoForStores = new ArrayList<>();
    for (OrderDelivery orderDelivery : orderDeliveryList) {
      List<ProductRead> productReadList = new ArrayList<>();
      for (OrderDeliveryProduct orderDeliveryProduct : orderDelivery.getOrderDeliveryProducts()) {
        ProductRead productRead = ProductRead.toDto(orderDeliveryProduct, productInfoDtoMap);
        productReadList.add(productRead);
      }
      OrderInfoForStore orderInfoForStore =
          OrderInfoForStore.toDto(orderDelivery, productReadList, storeNameMap, deliveryInfoMap);
      orderInfoForStores.add(orderInfoForStore);
    }

    // 배송정보는 일단 한 가게만 보여준다.
    DeliveryInfoDto deliveryInfoDto = deliveryInfoMap.get(deliveryIds.get(0));
    return OrderDeliveryGroup.toDto(orderGroupId, orderInfoForStores, paymentDate, deliveryInfoDto);
  }

  public OrderInfoForStoreForSeller getOrderDetailsForSeller(String orderDeliveryId) {
    OrderDelivery orderDelivery =
        orderDeliveryRepository.findById(orderDeliveryId).orElseThrow(EntityNotFoundException::new);

    // feign 요청
    List<String> productIds =
        orderDelivery.getOrderDeliveryProducts().stream()
            .map(OrderDeliveryProduct::getProductId)
            .collect(Collectors.toList());
    List<ProductInfoDto> productInfoDtos =
        productServiceClient.getProductInfo(productIds).getData();
    Map<String, ProductInfoDto> productInfoDtoMap =
        productInfoDtos.stream()
            .collect(Collectors.toMap(ProductInfoDto::getProductId, dto -> dto));

    List<Long> storeIds = new ArrayList<>();
    storeIds.add(orderDelivery.getStoreId());
    Map<Long, String> storeNameMap = storeServiceClient.getStoreName(storeIds).getData();

    List<Long> deliveryIds = new ArrayList<>();
    deliveryIds.add(orderDelivery.getDeliveryId());

    Map<Long, DeliveryInfoDto> deliveryInfoMap =
        deliveryServiceClient.getDeliveryInfo(deliveryIds).getData();

    String paymentDate =
        paymentServiceClient
            .getPaymentDate(orderDelivery.getOrderGroup().getOrderGroupId())
            .getData();

    // DTO 데이터 wrapping
    List<ProductRead> productReadList = new ArrayList<>();
    for (OrderDeliveryProduct orderDeliveryProduct : orderDelivery.getOrderDeliveryProducts()) {
      ProductRead productRead = ProductRead.toDto(orderDeliveryProduct, productInfoDtoMap);
      productReadList.add(productRead);
    }

    return OrderInfoForStoreForSeller.toDto(
        orderDelivery, productReadList, storeNameMap, paymentDate, deliveryInfoMap);
  }
}
