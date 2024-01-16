package kr.bb.order.kafka;

import bloomingblooms.domain.batch.SubscriptionBatchDtoList;
import bloomingblooms.domain.delivery.UpdateOrderStatusDto;
import bloomingblooms.domain.delivery.UpdateOrderSubscriptionStatusDto;
import bloomingblooms.domain.order.ProcessOrderDto;
import kr.bb.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumer<T> {
  private final OrderService orderService;
  private final KafkaProducer<ProcessOrderDto> kafkaProducer;

  @KafkaListener(topics = "order-create", groupId = "order")
  public void processOrder(ProcessOrderDto processOrderDto ) {
    try {
      orderService.processOrder(processOrderDto);
    } catch (Exception e) {
      // TODO : SQS & 문자로 주문 실패 알려주기 (주문&결제 실패시)
      log.error("proccess order failed rollback will begin. Error is : {}",e.toString());
      e.printStackTrace();
      log.error(String.valueOf(e.getCause()));

      // Kafka로 롤백 보상 패턴 실행
      kafkaProducer.send("order-create-rollback", processOrderDto);
    }
  }

  @KafkaListener(topics = "order-delivery-status", groupId = "order")
  public void updateOrderDeliveryStatus(UpdateOrderStatusDto updateOrderStatusDto) {
      orderService.updateOrderDeliveryStatus(updateOrderStatusDto);
  }

  @KafkaListener(topics = "order-subscription-status", groupId = "order")
  public void updateOrderSubscriptionStatus(
          UpdateOrderSubscriptionStatusDto updateOrderSubscriptionStatusDto ){
    orderService.updateOrderSubscriptionStatus(updateOrderSubscriptionStatusDto);
  }

  @KafkaListener(topics = "subscription-batch", groupId ="order")
  public void processSubscriptionBatch(SubscriptionBatchDtoList orderSubscriptionBatchDtoList){
    orderService.processSubscriptionBatch(orderSubscriptionBatchDtoList);
  }
}
