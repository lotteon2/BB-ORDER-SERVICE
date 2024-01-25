package kr.bb.order.service;

import bloomingblooms.domain.batch.SubscriptionBatchDto;
import bloomingblooms.domain.batch.SubscriptionBatchDtoList;
import bloomingblooms.domain.delivery.UpdateOrderStatusDto;
import bloomingblooms.domain.delivery.UpdateOrderSubscriptionStatusDto;
import bloomingblooms.domain.order.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityNotFoundException;
import kr.bb.order.entity.OrderDeliveryProduct;
import kr.bb.order.entity.OrderPickupProduct;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.entity.delivery.OrderGroup;
import kr.bb.order.entity.pickup.OrderPickup;
import kr.bb.order.entity.redis.OrderInfo;
import kr.bb.order.entity.redis.PickupOrderInfo;
import kr.bb.order.entity.redis.SubscriptionOrderInfo;
import kr.bb.order.entity.subscription.OrderSubscription;
import kr.bb.order.mapper.OrderProductMapper;
import kr.bb.order.repository.*;
import kr.bb.order.util.OrderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
  private final OrderDeliveryRepository orderDeliveryRepository;
  private final OrderDeliveryProductRepository orderDeliveryProductRepository;
  private final OrderPickupProductRepository orderPickupProductRepository;
  private final OrderGroupRepository orderGroupRepository;
  private final OrderPickupRepository orderPickupRepository;
  private final OrderSubscriptionRepository orderSubscriptionRepository;

  // (바로주문, 장바구니) 주문 저장하기
  @Transactional
  public OrderGroup createOrderDelivery(
      List<Long> deliveryIds,
      ProcessOrderDto processOrderDto,
      OrderInfo orderInfo) {

    OrderGroup orderGroup =
        OrderGroup.builder()
            .orderGroupId(processOrderDto.getOrderId())
            .userId(orderInfo.getUserId())
            .build();
    orderGroupRepository.save(orderGroup);

    // 주문 정보 저장
    for (int i = 0; i < orderInfo.getOrderInfoByStores().size(); i++) {
      // 1. 주문_배송 entity
      String orderDeliveryId = OrderUtil.generateUUID();
      OrderDelivery orderDelivery =
          OrderDelivery.toEntity(
              orderDeliveryId, deliveryIds.get(i), orderInfo.getOrderInfoByStores().get(i));
      // 연관관계 매핑 : 편의 메서드 적용
      orderDelivery.setOrderGroup(orderGroup);
      orderDeliveryRepository.save(orderDelivery);

      // 2. 주문_상품 entity
      List<OrderDeliveryProduct> orderDeliveryProducts = new ArrayList<>();
      for (ProductCreate productCreate : orderInfo.getOrderInfoByStores().get(i).getProducts()) {
        OrderDeliveryProduct orderDeliveryProduct = OrderProductMapper.toEntity(productCreate);
        // 연관관계 매핑 : 편의 메서드 적용
        orderDeliveryProduct.setOrderDelivery(orderDelivery);
        orderDeliveryProducts.add(orderDeliveryProduct);
      }
      orderDeliveryProductRepository.saveAll(orderDeliveryProducts);
    }

    return orderGroup;
  }

  // (픽업주문) 주문 저장하기
  @Transactional
  public OrderPickup createOrderPickup(
          ProcessOrderDto processOrderDto, PickupOrderInfo pickupOrderInfo) {

    LocalDateTime pickupDateTime =
        parseDateTime(pickupOrderInfo.getPickupDate(), pickupOrderInfo.getPickupTime());

    OrderPickup orderPickup =
        OrderPickup.builder()
            .orderPickupId(processOrderDto.getOrderId())
            .userId(pickupOrderInfo.getUserId())
            .storeId(pickupOrderInfo.getStoreId())
            .orderPickupTotalAmount(pickupOrderInfo.getTotalAmount())
            .orderPickupCouponAmount(pickupOrderInfo.getCouponAmount())
            .orderPickupDatetime(pickupDateTime)
            .orderPickupPhoneNumber(pickupOrderInfo.getOrdererPhoneNumber())
            .build();

    OrderPickupProduct orderPickupProduct =
        OrderPickupProduct.builder()
            .productId(pickupOrderInfo.getProduct().getProductId())
            .orderProductPrice(pickupOrderInfo.getProduct().getPrice())
            .orderProductQuantity(pickupOrderInfo.getQuantity())
            .build();

    orderPickupProduct.setOrderPickup(orderPickup);
    orderPickupProductRepository.save(orderPickupProduct);

    return orderPickup;
  }

  // (구독주문) 주문 저장하기
  @Transactional
  public OrderSubscription createOrderSubscription(
      List<Long> deliveryIds, ProcessOrderDto processOrderDto, SubscriptionOrderInfo subscriptionOrderInfo) {

    OrderSubscription orderSubscription =
        OrderSubscription.builder()
            .orderSubscriptionId(processOrderDto.getOrderId())
            .userId(processOrderDto.getUserId())
            .subscriptionProductId(subscriptionOrderInfo.getProduct().getProductId())
            .deliveryId(deliveryIds.get(0))
            .productName(subscriptionOrderInfo.getItemName())
            .productPrice(subscriptionOrderInfo.getProduct().getPrice())
            .deliveryDay(LocalDate.now().plusDays(3))
            .storeId(subscriptionOrderInfo.getStoreId())
            .phoneNumber(subscriptionOrderInfo.getOrdererPhoneNumber())
            .paymentDate(LocalDateTime.now().plusDays(30))
            .build();

    orderSubscriptionRepository.save(orderSubscription);

    return orderSubscription;
  }

  @Transactional(readOnly = true)
  public OrderDelivery readOrderDeliveryForStatusUpdate(UpdateOrderStatusDto statusDto) {
    OrderDelivery orderDelivery =
        orderDeliveryRepository
            .findById(statusDto.getOrderDeliveryId())
            .orElseThrow(EntityNotFoundException::new);
    orderDelivery.updateStatus(statusDto.getDeliveryStatus());
    return orderDelivery;
  }

  @Transactional(readOnly = true)
  public List<OrderSubscription> readOrderSubscriptionsForStatusChange(UpdateOrderSubscriptionStatusDto statusDto) {
     return orderSubscriptionRepository.findAllByDeliveryIds(statusDto.getDeliveryIds());
  }

  @Transactional(readOnly = true)
  public List<OrderSubscription> readOrderSubscriptionsForBatch(SubscriptionBatchDtoList subscriptionBatchDtoList) {
     return
        orderSubscriptionRepository.findAllById(
            subscriptionBatchDtoList.getSubscriptionBatchDtoList().stream()
                .map(SubscriptionBatchDto::getOrderSubscriptionId)
                .collect(Collectors.toList()));

  }

  // 픽업 상태 변경
  @Transactional(readOnly = true)
  public List<OrderPickup> readPickupsForStatusChange(LocalDateTime date) {
     return orderPickupRepository.findByOrderPickupDatetimeBetween(date.minusDays(1L), date);
  }

  private LocalDateTime parseDateTime(String pickupDate, String pickupTime) {
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    LocalDate date = LocalDate.parse(pickupDate, dateFormatter);
    LocalTime time = LocalTime.parse(pickupTime, timeFormatter);

    return LocalDateTime.of(date, time);
  }
}
