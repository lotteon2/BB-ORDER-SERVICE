package kr.bb.order.infra;

import bloomingblooms.domain.notification.NotificationData;
import bloomingblooms.domain.notification.NotificationKind;
import bloomingblooms.domain.notification.NotificationURL;
import bloomingblooms.domain.notification.PublishNotificationInformation;
import bloomingblooms.domain.order.OrderStatusNotification;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderSQSPublisher {
  private final AmazonSQS sqs;
  private final ObjectMapper objectMapper;

  @Value("${cloud.aws.sqs.new-order-status-queue.url}")
  private String queueUrl;

  public void publish(Long userId, String phoneNumber) {
    try {
      OrderStatusNotification orderStatusNotification =
          OrderStatusNotification.builder().userId(userId).phoneNumber(phoneNumber).build();
      PublishNotificationInformation publishNotificationInformation =
          PublishNotificationInformation.getData(
              NotificationURL.ORDER_SUCCESS, NotificationKind.ORDER_SUCCESS);
      NotificationData<OrderStatusNotification> orderStatusNotificationNotificationData =
          NotificationData.notifyData(orderStatusNotification, publishNotificationInformation);
      SendMessageRequest sendMessageRequest =
          new SendMessageRequest(
              queueUrl, objectMapper.writeValueAsString(orderStatusNotificationNotificationData));
      sqs.sendMessage(sendMessageRequest);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
