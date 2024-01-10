package kr.bb.order.service;

import bloomingblooms.domain.delivery.DeliveryInfoDto;
import bloomingblooms.domain.product.ProductInformation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.persistence.EntityNotFoundException;
import kr.bb.order.dto.response.order.WeeklySalesInfoDto;
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
import kr.bb.order.repository.OrderPickupRepository;
import kr.bb.order.repository.OrderSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class OrderDetailsService {
  private final OrderDeliveryRepository orderDeliveryRepository;
  private final OrderPickupRepository orderPickupRepository;
  private final OrderSubscriptionRepository orderSubscriptionRepository;
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
    List<ProductInformation> productInformations =
        productServiceClient.getProductInfo(productIds).getData();
    Map<String, ProductInformation> productInfoDtoMap =
        productInformations.stream()
            .collect(Collectors.toMap(ProductInformation::getProductId, dto -> dto));

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
    List<ProductInformation> productInformations =
        productServiceClient.getProductInfo(productIds).getData();
    Map<String, ProductInformation> productInfoDtoMap =
        productInformations.stream()
            .collect(Collectors.toMap(ProductInformation::getProductId, dto -> dto));

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

  public WeeklySalesInfoDto getWeeklySalesInfo(Long storeId) {
    LocalDateTime endDate = LocalDateTime.now().minusDays(1); // 어제 날짜
    LocalDateTime startDate = endDate.minusDays(6); // 7일 전

    List<Object[]> weeklySalesDataForDelivery =
        orderDeliveryRepository.findWeeklySales(storeId, startDate, endDate);
    List<Object[]> weeklySalesDataForPickup =
        orderPickupRepository.findWeeklySales(storeId, startDate, endDate);
    List<Object[]> weeklySalesDataForSubscription =
        orderSubscriptionRepository.findWeeklySales(storeId, startDate, endDate);

    List<String> lastSevenDays = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      LocalDate date = startDate.toLocalDate().plusDays(i);
      String formattedDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
      lastSevenDays.add(formattedDate);
    }

    // 총 7일치 매출 선언 및 초기화
    LinkedHashMap<String, Long> weeklySalesMap = new LinkedHashMap<>();
    for (int i = 0; i < 7; i++) {
      weeklySalesMap.put(lastSevenDays.get(i), 0L);
    }

    for (Object[] objectsForDelivery : weeklySalesDataForDelivery) {
      LocalDateTime localDateTime = (LocalDateTime) objectsForDelivery[0];
      String key = localDateTime.toLocalDate().toString();
      Long amount = weeklySalesMap.get(key);
      weeklySalesMap.put(key, amount + Long.valueOf(objectsForDelivery[1].toString()));
    }

    for (Object[] objectsForPickup : weeklySalesDataForPickup) {
      LocalDateTime localDateTime = (LocalDateTime) objectsForPickup[0];
      String key = localDateTime.toLocalDate().toString();
      Long amount = weeklySalesMap.get(key);
      weeklySalesMap.put(key, amount + Long.valueOf(objectsForPickup[1].toString()));
    }

    for (Object[] objectsForSubscription : weeklySalesDataForSubscription) {
      LocalDateTime localDateTime = (LocalDateTime) objectsForSubscription[0];
      String key = localDateTime.toLocalDate().toString();
      Long amount = weeklySalesMap.get(key);
      weeklySalesMap.put(key, amount + Long.valueOf((objectsForSubscription[1].toString())));
    }

    List<String> dates = new ArrayList<>();
    List<Long> totalAmounts = new ArrayList<>();
    for (Entry<String, Long> elements : weeklySalesMap.entrySet()) {
      if (elements.getValue() == 0L) continue;
      dates.add(elements.getKey());
      totalAmounts.add(elements.getValue());
    }
    return WeeklySalesInfoDto.builder().categories(dates).data(totalAmounts).build();
  }

  public Long getDeliveryId(String orderId) {
    OrderDelivery orderDelivery =
        orderDeliveryRepository.findById(orderId).orElseThrow(EntityNotFoundException::new);
    return orderDelivery.getDeliveryId();
  }
}
