package kr.bb.order.kafka;

import bloomingblooms.domain.delivery.UpdateOrderStatusDto;
import bloomingblooms.domain.order.ProcessOrderDto;
import kr.bb.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaConsumer<T> {
  private final OrderService orderService;
  private final KafkaProducer<ProcessOrderDto> kafkaProducer;

  @KafkaListener(topics = "order-create")
  public void processOrder(ProcessOrderDto processOrderDto ) {
    try {
      orderService.processOrder(processOrderDto);
    } catch (Exception e) {
      // TODO : SQS & 문자로 주문 실패 알려주기 (주문&결제 실패시)


      // Kafka로 롤백 보상 패턴 실행
      kafkaProducer.send("order-create-rollback", processOrderDto);
      throw e;
    }
  }

  @KafkaListener(topics = "order-delivery-status")
  public void updateOrderDeliveryStatus(UpdateOrderStatusDto updateOrderStatusDto) {
      orderService.updateStatus(updateOrderStatusDto);
  }
}
