package kr.bb.order.mapper;

import bloomingblooms.domain.delivery.DeliveryInsertDto;
import bloomingblooms.domain.order.OrderInfoByStore;
import bloomingblooms.domain.order.ProcessOrderDto;
import bloomingblooms.domain.order.ProductCreate;
import bloomingblooms.domain.pickup.PickupCreateDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kr.bb.order.entity.OrderPickupProduct;
import kr.bb.order.entity.pickup.OrderPickupStatus;
import kr.bb.order.entity.redis.OrderInfo;
import kr.bb.order.entity.redis.PickupOrderInfo;

public class OrderCommonMapper {
  public static List<DeliveryInsertDto> toDto(OrderInfo orderInfo) {
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
}
