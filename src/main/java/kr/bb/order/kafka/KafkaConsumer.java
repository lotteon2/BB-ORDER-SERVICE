package kr.bb.order.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.bb.order.dto.kafka.ProcessOrderDto;
import kr.bb.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaConsumer {
  private final ObjectMapper objectMapper;
  private final OrderService orderService;

  @KafkaListener(topics = "process-order")
  public void processOrder(String message) {
    try {
      ProcessOrderDto processOrderDto = objectMapper.readValue(message, ProcessOrderDto.class);
      orderService.processOrder(processOrderDto);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
