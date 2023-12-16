package kr.bb.order.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.bb.order.dto.request.store.ProcessOrderDto;
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
      } catch(Exception e){
        // TODO : SQS & 문자로 주문 실패 알려주기 (주문&결제 실패시)
        //
        kafkaProducer.rollbackOrder(processOrderDto);
      }
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  // TODO: SQS 알림 보내는 방법 상원이한테 물어보기 (가게 실패시)
  @KafkaListener(topics = "request-order-rollback")
  public void rollbackOrder(String message){

  }

}
