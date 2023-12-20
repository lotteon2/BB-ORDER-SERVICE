package kr.bb.order.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.bb.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaConsumer {
  private final ObjectMapper objectMapper;
  private final OrderService orderService;
  private final KafkaProducer kafkaProducer;

  @KafkaListener(topics = "process-order")
  public void processOrder(String message) {
    try {
      ProcessOrderDto processOrderDto = objectMapper.readValue(message, ProcessOrderDto.class);
      try {
        orderService.processOrder(processOrderDto);
      } catch (Exception e) {
        // TODO : SQS & 문자로 주문 실패 알려주기 (주문&결제 실패시)
        //
        kafkaProducer.rollbackOrder(processOrderDto);
        throw e;
      }
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @KafkaListener(topics = "order-delivery-status")
  public void updateOrderDeliveryStatus(String message) {
    try {
      UpdateOrderStatusDto updateOrderStatusDto = objectMapper.readValue(message, UpdateOrderStatusDto.class);
      orderService.updateStatus(updateOrderStatusDto);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
