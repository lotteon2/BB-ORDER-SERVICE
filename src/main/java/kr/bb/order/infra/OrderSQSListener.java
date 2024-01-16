package kr.bb.order.infra;

import bloomingblooms.domain.StatusChangeDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import javax.annotation.security.PermitAll;
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

  // (픽업주문) 카드 상태 변경
  @SqsListener(
          value="${cloud.aws.sqs.pickup-card-status-order-queue.name}",
          deletionPolicy = SqsMessageDeletionPolicy.NEVER)
  public void consumeCardDataUpdateForPickupQueue(
          @Payload String message, Acknowledgment ack) throws JsonProcessingException {
    StatusChangeDto statusChangeDto = objectMapper.readValue(message, StatusChangeDto.class);
    orderSqsService.updateOrderPickupCard(statusChangeDto);
    ack.acknowledge();
  }

  // (픽업주문) 리뷰 상태 변경
  @SqsListener(
          value="${cloud.aws.sqs.pickup-review-status-order-queue.name}"
          , deletionPolicy = SqsMessageDeletionPolicy.NEVER)
  public void consumeReviewDataUpdateForPickupQueue(
          @Payload String message, Acknowledgment ack) throws JsonProcessingException{
    StatusChangeDto statusChangeDto = objectMapper.readValue(message, StatusChangeDto.class);
    orderSqsService.updateOrderPickupReview(statusChangeDto);
    ack.acknowledge();
  }
}
