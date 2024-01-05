package kr.bb.order.mapper;

import bloomingblooms.domain.delivery.DeliveryInsertDto;
import bloomingblooms.domain.notification.order.OrderType;
import bloomingblooms.domain.order.NewOrderEvent.NewOrderEventItem;
import bloomingblooms.domain.order.NewOrderEvent.ProductCount;
import bloomingblooms.domain.order.OrderInfoByStore;
import bloomingblooms.domain.order.OrderMethod;
import bloomingblooms.domain.order.ProcessOrderDto;
import bloomingblooms.domain.order.ProductCreate;
import bloomingblooms.domain.pickup.PickupCreateDto;
import bloomingblooms.domain.subscription.SubscriptionCreateDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kr.bb.order.entity.OrderDeliveryProduct;
import kr.bb.order.entity.OrderPickupProduct;
import kr.bb.order.entity.ReviewStatus;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.entity.delivery.OrderGroup;
import kr.bb.order.entity.pickup.OrderPickup;
import kr.bb.order.entity.pickup.OrderPickupStatus;
import kr.bb.order.entity.redis.OrderInfo;
import kr.bb.order.entity.redis.PickupOrderInfo;
import kr.bb.order.entity.redis.SubscriptionOrderInfo;
import kr.bb.order.entity.subscription.OrderSubscription;

public class OrderCommonMapper {
  public static List<DeliveryInsertDto> toDeliveryInsertDto(OrderInfo orderInfo) {
    List<OrderInfoByStore> orderInfoByStores = orderInfo.getOrderInfoByStores();
    List<DeliveryInsertDto> list = new ArrayList<>();

    for (OrderInfoByStore orderInfoByStore : orderInfoByStores) {
      DeliveryInsertDto dto =
          DeliveryInsertDto.builder()
              .ordererName(orderInfo.getOrdererName())
              .ordererPhoneNumber(orderInfo.getOrdererPhoneNumber())
              .ordererEmail(orderInfo.getOrdererEmail())
              .recipientName(orderInfo.getRecipientName())
              .recipientPhoneNumber(orderInfo.getRecipientPhone())
              .zipcode(orderInfo.getDeliveryZipcode())
              .roadName(orderInfo.getDeliveryRoadName())
              .addressDetail(orderInfo.getDeliveryAddressDetail())
              .request(orderInfo.getDeliveryRequest())
              .deliveryCost(orderInfoByStore.getDeliveryCost())
              .build();
      list.add(dto);
    }

    return list;
  }

  public static List<DeliveryInsertDto> toDeliveryInsertDtoForSubscription(
      SubscriptionOrderInfo subscriptionOrderInfo) {
    DeliveryInsertDto dto =
        DeliveryInsertDto.builder()
            .ordererName(subscriptionOrderInfo.getOrdererName())
            .ordererPhoneNumber(subscriptionOrderInfo.getOrdererPhoneNumber())
            .ordererEmail(subscriptionOrderInfo.getOrdererEmail())
            .recipientName(subscriptionOrderInfo.getRecipientName())
            .recipientPhoneNumber(subscriptionOrderInfo.getOrdererPhoneNumber())
            .zipcode(subscriptionOrderInfo.getDeliveryZipcode())
            .roadName(subscriptionOrderInfo.getDeliveryRoadName())
            .addressDetail(subscriptionOrderInfo.getDeliveryAddressDetail())
            .request(subscriptionOrderInfo.getDeliveryRequest())
            .deliveryCost(subscriptionOrderInfo.getDeliveryCost())
            .build();
    List<DeliveryInsertDto> list = List.of(dto);
    return list;
  }

  public static ProcessOrderDto toProcessOrderDto(
      String orderId, String orderType, OrderInfo orderInfo) {

    List<Long> couponIds =
        orderInfo.getOrderInfoByStores().stream()
            .map(OrderInfoByStore::getCouponId)
            .collect(Collectors.toList());
    Map<String, Long> products =
        orderInfo.getOrderInfoByStores().stream()
            .flatMap(orderInfoByStore -> orderInfoByStore.getProducts().stream())
            .collect(Collectors.toMap(ProductCreate::getProductId, ProductCreate::getQuantity));

    return ProcessOrderDto.builder()
        .orderId(orderId)
        .orderType(orderType)
        .couponIds(couponIds)
        .products(products)
        .userId(orderInfo.getUserId())
        .phoneNumber(orderInfo.getOrdererPhoneNumber())
        .orderMethod(orderInfo.getOrderMethod())
        .build();
  }

  public static ProcessOrderDto toDtoForOrderPickup(
      String orderId, PickupOrderInfo pickupOrderInfo) {
    List<Long> couponIds = List.of(pickupOrderInfo.getCouponId());
    Map<String, Long> product =
        Map.of(
            pickupOrderInfo.getProduct().getProductId(),
            pickupOrderInfo.getProduct().getQuantity());

    return ProcessOrderDto.builder()
        .orderId(orderId)
        .orderType(pickupOrderInfo.getOrderType())
        .couponIds(couponIds)
        .products(product)
        .userId(pickupOrderInfo.getUserId())
        .phoneNumber(pickupOrderInfo.getOrdererPhoneNumber())
        .orderMethod(OrderMethod.DIRECT.getOrderMethod())
        .build();
  }

  public static ProcessOrderDto toDtoForOrderSubscription(
      String orderId, SubscriptionOrderInfo subscriptionOrderInfo) {
    List<Long> couponIds = List.of(subscriptionOrderInfo.getCouponId());
    Map<String, Long> product =
        Map.of(
            subscriptionOrderInfo.getProduct().getProductId(),
            subscriptionOrderInfo.getProduct().getQuantity());

    return ProcessOrderDto.builder()
        .orderId(orderId)
        .orderType(subscriptionOrderInfo.getOrderType())
        .couponIds(couponIds)
        .products(product)
        .userId(subscriptionOrderInfo.getUserId())
        .phoneNumber(subscriptionOrderInfo.getOrdererPhoneNumber())
        .orderMethod(OrderMethod.DIRECT.getOrderMethod())
        .build();
  }

  public static PickupCreateDto toPickupCreateDto(
      PickupOrderInfo pickupOrderInfo,
      LocalDateTime paymentDateTime,
      OrderPickupProduct orderPickupProduct) {
    return PickupCreateDto.builder()
        .pickupReservationId(pickupOrderInfo.getTempOrderId())
        .userId(pickupOrderInfo.getUserId())
        .storeId(pickupOrderInfo.getStoreId())
        .productId(pickupOrderInfo.getProduct().getProductId())
        .ordererName(pickupOrderInfo.getOrdererName())
        .ordererPhoneNumber(pickupOrderInfo.getOrdererPhoneNumber())
        .ordererEmail(pickupOrderInfo.getOrdererEmail())
        .quantity(Math.toIntExact(pickupOrderInfo.getQuantity()))
        .pickupDate(LocalDate.parse(pickupOrderInfo.getPickupDate()))
        .pickupTime(pickupOrderInfo.getPickupTime())
        .totalOrderPrice(pickupOrderInfo.getTotalAmount())
        .totalDiscountPrice(pickupOrderInfo.getCouponAmount())
        .actualPrice(pickupOrderInfo.getActualAmount())
        .paymentDateTime(paymentDateTime)
        .reservationStatus(OrderPickupStatus.PENDING.getMessage())
        .reviewStatus(orderPickupProduct.getReviewIsWritten().toString())
        .cardStatus(orderPickupProduct.getCardIsWritten().toString())
        .productThumbnail(pickupOrderInfo.getProduct().getProductThumbnailImage())
        .productName(pickupOrderInfo.getProduct().getProductName())
        .unitPrice(orderPickupProduct.getOrderProductPrice())
        .build();
  }

  public static SubscriptionCreateDto toSubscriptionCreateDto(
      SubscriptionOrderInfo subscriptionOrderInfo,
      LocalDateTime paymentDateTime,
      OrderSubscription orderSubscription) {
    return SubscriptionCreateDto.builder()
        .subscriptionId(orderSubscription.getOrderSubscriptionId())
        .userId(orderSubscription.getUserId())
        .storeId(subscriptionOrderInfo.getStoreId())
        .productId(orderSubscription.getSubscriptionProductId())
        .quantity(Math.toIntExact(subscriptionOrderInfo.getQuantity()))
        .ordererName(subscriptionOrderInfo.getOrdererName())
        .ordererPhoneNumber(subscriptionOrderInfo.getOrdererPhoneNumber())
        .ordererEmail(subscriptionOrderInfo.getOrdererEmail())
        .recipientName(subscriptionOrderInfo.getRecipientName())
        .recipientPhoneNumber(subscriptionOrderInfo.getRecipientPhone())
        .storeName(subscriptionOrderInfo.getStoreName())
        .zipcode(subscriptionOrderInfo.getDeliveryZipcode())
        .roadName(subscriptionOrderInfo.getDeliveryRoadName())
        .addressDetail(subscriptionOrderInfo.getDeliveryAddressDetail())
        .paymentDateTime(paymentDateTime)
        .nextDeliveryDate(LocalDate.now().plusDays(3))
        .nextPaymentDate(LocalDate.now().plusDays(30))
        .totalOrderPrice(subscriptionOrderInfo.getTotalAmount())
        .totalDiscountPrice(subscriptionOrderInfo.getCouponAmount())
        .deliveryPrice(subscriptionOrderInfo.getDeliveryCost())
        .actualPrice(subscriptionOrderInfo.getActualAmount())
        .reviewStatus(ReviewStatus.DISABLED.toString())
        .deliveryRequest(subscriptionOrderInfo.getDeliveryRequest())
        .productThumbnail(subscriptionOrderInfo.getProduct().getProductThumbnailImage())
        .productName(subscriptionOrderInfo.getProduct().getProductName())
        .unitPrice(subscriptionOrderInfo.getProduct().getPrice())
        .build();
  }

  public static List<NewOrderEventItem> createNewOrderEventListForDelivery(
      OrderGroup orderGroup, OrderInfo orderInfo) {
    List<OrderDelivery> orderDeliveryList = orderGroup.getOrderDeliveryList();

    List<NewOrderEventItem> newOrderEventItems = new ArrayList<>();
    for (OrderDelivery orderDelivery : orderDeliveryList) {
      List<ProductCount> productCountList = new ArrayList<>();
      for (OrderDeliveryProduct orderDeliveryProduct : orderDelivery.getOrderDeliveryProducts()) {
        productCountList.add(
            ProductCount.builder()
                .productId(orderDeliveryProduct.getProductId())
                .quantity(orderDeliveryProduct.getOrderProductQuantity())
                .build());
      }
      NewOrderEventItem newOrderEventItem =
          NewOrderEventItem.builder()
              .orderId(orderDelivery.getOrderDeliveryId())
              .productName(orderInfo.getItemName())
              .storeId(orderDelivery.getStoreId())
              .orderType(OrderType.valueOf(orderInfo.getOrderType()))
              .products(productCountList)
              .build();
      newOrderEventItems.add(newOrderEventItem);
    }
    return newOrderEventItems;
  }

  public static List<NewOrderEventItem> createNewOrderEventListForPickup(
      OrderPickup orderPickup, PickupOrderInfo pickupOrderInfo) {
    ProductCount productCount =
        ProductCount.builder()
            .productId(orderPickup.getOrderPickupProduct().getProductId())
            .quantity(orderPickup.getOrderPickupProduct().getOrderProductQuantity())
            .build();
    List<ProductCount> productCountList = List.of(productCount);
    NewOrderEventItem newOrderEventItem =
        NewOrderEventItem.builder()
            .orderId(pickupOrderInfo.getTempOrderId())
            .productName(pickupOrderInfo.getItemName())
            .storeId(pickupOrderInfo.getStoreId())
            .orderType(OrderType.valueOf(pickupOrderInfo.getOrderType()))
            .products(productCountList)
            .build();

    return List.of(newOrderEventItem);
  }

  public static List<NewOrderEventItem> createNewOrderEventListForSubscription(
      OrderSubscription orderSubscription, SubscriptionOrderInfo subscriptionOrderInfo) {
    ProductCount productCount =
        ProductCount.builder()
            .productId(orderSubscription.getSubscriptionProductId())
            .quantity(subscriptionOrderInfo.getQuantity())
            .build();
    List<ProductCount> productCountList = List.of(productCount);
    NewOrderEventItem newOrderEventItem =
        NewOrderEventItem.builder()
            .orderId(subscriptionOrderInfo.getTempOrderId())
            .productName(orderSubscription.getProductName())
            .storeId(subscriptionOrderInfo.getStoreId())
            .orderType(OrderType.valueOf(subscriptionOrderInfo.getOrderType()))
            .products(productCountList)
            .build();
    return List.of(newOrderEventItem);
  }
}
