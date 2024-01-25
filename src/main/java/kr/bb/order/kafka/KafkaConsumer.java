package kr.bb.order.kafka;

import bloomingblooms.domain.batch.SubscriptionBatchDtoList;
import bloomingblooms.domain.delivery.UpdateOrderStatusDto;
import bloomingblooms.domain.delivery.UpdateOrderSubscriptionStatusDto;
import bloomingblooms.domain.order.ProcessOrderDto;
import kr.bb.order.facade.OrderFacade;
import kr.bb.order.infra.OrderSQSPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumer<T> {
  private final OrderFacade orderFacade;
  private final KafkaProducer<ProcessOrderDto> kafkaProducer;
  private final OrderSQSPublisher orderSQSPublisher;

  @KafkaListener(topics = "order-create", groupId = "order")
  public void processOrder(ProcessOrderDto processOrderDto ) {
    try {
      orderFacade.processOrder(processOrderDto);
    } catch (Exception e) {
      log.error("proccess order failed rollback will begin. Error is : {}",e.toString());

      // SQS로 고객에게 주문 실패 알림 (주문&결제 실패시)
      orderSQSPublisher.publishOrderFail(processOrderDto.getUserId(), processOrderDto.getPhoneNumber());

      // Kafka로 롤백 보상 패턴 실행
      kafkaProducer.send("order-create-rollback", processOrderDto);
    }
  }

  @KafkaListener(topics = "order-delivery-status", groupId = "order")
  public void updateOrderDeliveryStatus(UpdateOrderStatusDto updateOrderStatusDto) {
    orderFacade.updateOrderDeliveryStatus(updateOrderStatusDto);
  }

  @KafkaListener(topics = "order-subscription-status", groupId = "order")
  public void updateOrderSubscriptionStatus(
          UpdateOrderSubscriptionStatusDto updateOrderSubscriptionStatusDto ){
    orderFacade.updateOrderSubscriptionStatus(updateOrderSubscriptionStatusDto);
  }

  @KafkaListener(topics = "subscription-batch", groupId ="order")
  public void processSubscriptionBatch(SubscriptionBatchDtoList orderSubscriptionBatchDtoList){
    orderFacade.processSubscriptionBatch(orderSubscriptionBatchDtoList);
  }
}
