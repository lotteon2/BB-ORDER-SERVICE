package kr.bb.order.service;

import bloomingblooms.domain.delivery.DeliveryInfoDto;
import bloomingblooms.domain.notification.delivery.DeliveryStatus;
import bloomingblooms.domain.notification.order.OrderType;
import bloomingblooms.domain.order.PickupStatusChangeDto;
import bloomingblooms.domain.order.ProcessOrderDto;
import bloomingblooms.domain.order.SubscriptionStatusChangeDto;
import bloomingblooms.domain.review.ReviewStatus;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityNotFoundException;
import kr.bb.order.dto.feign.KakaopayCancelRequestDto;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.entity.pickup.OrderPickup;
import kr.bb.order.entity.pickup.OrderPickupStatus;
import kr.bb.order.entity.subscription.OrderSubscription;
import kr.bb.order.entity.subscription.SubscriptionStatus;
import kr.bb.order.feign.DeliveryServiceClient;
import kr.bb.order.feign.FeignHandler;
import kr.bb.order.infra.OrderSQSPublisher;
import kr.bb.order.kafka.KafkaProducer;
import kr.bb.order.repository.OrderDeliveryRepository;
import kr.bb.order.repository.OrderPickupRepository;
import kr.bb.order.repository.OrderSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCancelService {
  private final OrderDeliveryRepository orderDeliveryRepository;
  private final OrderPickupRepository orderPickupRepository;
  private final OrderSubscriptionRepository orderSubscriptionRepository;
  private final DeliveryServiceClient deliveryServiceClient;
  private final FeignHandler feignHandler;
  private final KafkaProducer<ProcessOrderDto> kafkaProducer;
  private final KafkaProducer<PickupStatusChangeDto> kafkaProducerForOrderPickup;
  private final KafkaProducer<SubscriptionStatusChangeDto> kakfaProducerForOrderSubscription;
  private final OrderSQSPublisher orderSQSPublisher;

  @Transactional
  public void cancelOrderDelivery(String orderDeliveryId) {
    OrderDelivery orderDelivery =
        orderDeliveryRepository.findById(orderDeliveryId).orElseThrow(EntityNotFoundException::new);

    String orderGroupId = orderDelivery.getOrderGroup().getOrderGroupId();

    // delivery-service로 특정 가게 배송비정보 요청
    List<Long> deliveryIds = List.of(orderDelivery.getDeliveryId());
    Map<Long, DeliveryInfoDto> deliveryInfoDtoMap =
        deliveryServiceClient.getDeliveryInfo(deliveryIds).getData();
    Long key = orderDelivery.getDeliveryId();

    Long paymentAmount =
        orderDelivery.getOrderDeliveryTotalAmount()
            - orderDelivery.getOrderDeliveryCouponAmount()
            + deliveryInfoDtoMap.get(key).getDeliveryCost();

    KakaopayCancelRequestDto requestDto =
        KakaopayCancelRequestDto.builder()
            .orderId(orderGroupId)
            .cancelAmount(paymentAmount)
            .build();

    feignHandler.cancel(requestDto);

    // 가게주문 상태를 취소로 변경
    orderDelivery.updateStatus(DeliveryStatus.CANCELED);

    // Rollback 요청
    Map<String, Long> products = new HashMap<>();
    orderDelivery
        .getOrderDeliveryProducts()
        .forEach(
            orderDeliveryProduct ->
                products.put(
                    orderDeliveryProduct.getProductId(),
                    orderDeliveryProduct.getOrderProductQuantity()));

    ProcessOrderDto processOrderDto =
        ProcessOrderDto.builder()
            .orderId(orderDeliveryId)
            .orderType(OrderType.DELIVERY.toString())
            .orderMethod("")
            .couponIds(Collections.emptyList())
            .products(products)
            .userId(orderDelivery.getOrderGroup().getUserId())
            .phoneNumber(deliveryInfoDtoMap.get(key).getOrdererPhone())
            .build();

    kafkaProducer.send("order-create-rollback", processOrderDto);

    // 주문 취소 알림 발송
    orderSQSPublisher.publishOrderCancel(orderDelivery.getStoreId(), OrderType.DELIVERY);
  }

  @Transactional
  public void cancelOrderPickup(String orderPickupId) {
    OrderPickup orderPickup =
        orderPickupRepository.findById(orderPickupId).orElseThrow(EntityNotFoundException::new);

    Long paymentAmount =
        orderPickup.getOrderPickupTotalAmount() - orderPickup.getOrderPickupCouponAmount();

    KakaopayCancelRequestDto requestDto =
        KakaopayCancelRequestDto.builder()
            .orderId(orderPickupId)
            .cancelAmount(paymentAmount)
            .build();

    feignHandler.cancel(requestDto);

    // 픽업주문 상태를 취소로 변경
    orderPickup.updateStatus(OrderPickupStatus.CANCELED);
    orderPickup.getOrderPickupProduct().updateCardAndReviewStatus(OrderPickupStatus.CANCELED);

    // Rollback 요청
    Map<String, Long> products = new HashMap<>();
    products.put(
        orderPickup.getOrderPickupProduct().getProductId(),
        orderPickup.getOrderPickupProduct().getOrderProductQuantity());

    ProcessOrderDto processOrderDto =
        ProcessOrderDto.builder()
            .orderId(orderPickupId)
            .orderType(OrderType.PICKUP.toString())
            .orderMethod("")
            .couponIds(Collections.emptyList())
            .products(products)
            .userId(orderPickup.getUserId())
            .phoneNumber(orderPickup.getOrderPickupPhoneNumber())
            .build();

    kafkaProducer.send("order-create-rollback", processOrderDto);

    // Order-Query로 픽업주문 데이터 update
    PickupStatusChangeDto pickupStatusChangeDto =
        PickupStatusChangeDto.builder()
            .orderId(orderPickupId)
            .pickupStatus(OrderPickupStatus.CANCELED.toString())
            .cardStatus(orderPickup.getOrderPickupProduct().getCardIsWritten())
            .reviewStatus(orderPickup.getOrderPickupProduct().getReviewIsWritten())
            .build();
    kafkaProducerForOrderPickup.send("pickup-status-update", pickupStatusChangeDto);

    // 주문 취소 알림 발송
    orderSQSPublisher.publishOrderCancel(orderPickup.getStoreId(), OrderType.PICKUP);
  }

  @Transactional
  public void cancelOrderSubscription(String orderSubscriptionId) {
    OrderSubscription orderSubscription =
        orderSubscriptionRepository
            .findById(orderSubscriptionId)
            .orElseThrow(EntityNotFoundException::new);

    // delivery-service로 배송비 조회

    List<Long> deliveryIds = List.of(orderSubscription.getDeliveryId());
    Map<Long, DeliveryInfoDto> deliveryInfoDtoMap =
        deliveryServiceClient.getDeliveryInfo(deliveryIds).getData();
    Long key = orderSubscription.getDeliveryId();

    Long paymentAmount =
        orderSubscription.getProductPrice() + deliveryInfoDtoMap.get(key).getDeliveryCost();

    log.warn("취소 주문 금액: {}", paymentAmount);

    KakaopayCancelRequestDto requestDto =
        KakaopayCancelRequestDto.builder()
            .orderId(orderSubscriptionId)
            .cancelAmount(paymentAmount)
            .build();

    feignHandler.cancelSubscription(requestDto);

    // 구독주문 상태를 취소로 변경
    orderSubscription.updateStatus(SubscriptionStatus.CANCELED);
    orderSubscription.updateReviewStatus(ReviewStatus.DISABLED);

    // order-query로 kafka send
    SubscriptionStatusChangeDto subscriptionStatusChangeDto = SubscriptionStatusChangeDto.builder()
            .orderId(orderSubscriptionId)
            .subscriptionStatus(SubscriptionStatus.CANCELED.toString())
            .reviewStatus(ReviewStatus.DISABLED)
            .build();
    kakfaProducerForOrderSubscription.send("subscription-status-update", subscriptionStatusChangeDto);

    // Rollback 요청
    Map<String, Long> products = new HashMap<>();
    products.put(orderSubscription.getSubscriptionProductId(), 1L);

    ProcessOrderDto processOrderDto =
        ProcessOrderDto.builder()
            .orderId(orderSubscriptionId)
            .orderType(OrderType.SUBSCRIBE.toString())
            .orderMethod("")
            .couponIds(Collections.emptyList())
            .products(products)
            .userId(orderSubscription.getUserId())
            .phoneNumber(orderSubscription.getPhoneNumber())
            .build();

    kafkaProducer.send("order-create-rollback", processOrderDto);

    // 주문 취소 알림
    orderSQSPublisher.publishOrderCancel(orderSubscription.getStoreId(), OrderType.SUBSCRIBE);
  }
}
