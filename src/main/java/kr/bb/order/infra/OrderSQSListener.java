package kr.bb.order.infra;

import bloomingblooms.domain.StatusChangeDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import kr.bb.order.service.OrderSqsService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.aws.messaging.listener.Acknowledgment;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderSQSListener {
  private final ObjectMapper objectMapper;
  private final OrderSqsService orderSqsService;

  // 리뷰 상태 변경
  @SqsListener(
      value = "${cloud.aws.sqs.delivery-review-status-queue.name}",
      deletionPolicy = SqsMessageDeletionPolicy.NEVER)
  public void consumeReviewDataUpdateQueue(
      @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack)
      throws JsonProcessingException {
    String messageFromSNS = getMessageFromSNS(message);
    StatusChangeDto statusChangeDto = objectMapper.readValue(messageFromSNS, StatusChangeDto.class);

    orderSqsService.updateOrderDeliveryReview(statusChangeDto);
    ack.acknowledge();
  }

  // 카드 상태 변경
  @SqsListener(
      value = "${cloud.aws.sqs.card-is-register-for-order-history-queue.name}",
      deletionPolicy = SqsMessageDeletionPolicy.NEVER)
  public void consumeCardDataUpdateQueue(
      @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack)
          throws JsonProcessingException {
      String messageFromSNS = getMessageFromSNS(message);
      StatusChangeDto statusChangeDto  = objectMapper.readValue(messageFromSNS, StatusChangeDto.class);

      orderSqsService.updateOrderDeliveryCard(statusChangeDto);
  }

  private String getMessageFromSNS(String message) throws JsonProcessingException {
    JsonNode jsonNode = objectMapper.readTree(message);
    return jsonNode.get("Message").asText();
  }
}
