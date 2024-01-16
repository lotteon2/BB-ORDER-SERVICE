package kr.bb.order.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import kr.bb.order.dto.ProductStatusChangeDto;
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

  // (배송주문) 리뷰 상태 변경
  @SqsListener(
      value = "${cloud.aws.sqs.delivery-review-status-queue.name}",
      deletionPolicy = SqsMessageDeletionPolicy.NEVER)
  public void consumeReviewDataUpdateQueue(
      @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack)
      throws JsonProcessingException {
    ProductStatusChangeDto statusChangeDto = objectMapper.readValue(message, ProductStatusChangeDto.class);

    orderSqsService.updateOrderDeliveryReview(statusChangeDto);
    ack.acknowledge();
  }

  // (배송주문) 카드 상태 변경
  @SqsListener(
      value = "${cloud.aws.sqs.card-is-register-for-order-history-queue.name}",
      deletionPolicy = SqsMessageDeletionPolicy.NEVER)
  public void consumeCardDataUpdateQueue(
      @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack)
      throws JsonProcessingException {
    ProductStatusChangeDto statusChangeDto = objectMapper.readValue(message, ProductStatusChangeDto.class);

    orderSqsService.updateOrderDeliveryCard(statusChangeDto);
    ack.acknowledge();
  }

}
