package kr.bb.order.infra;

import bloomingblooms.domain.notification.NotificationData;
import bloomingblooms.domain.notification.NotificationKind;
import bloomingblooms.domain.notification.NotificationURL;
import bloomingblooms.domain.notification.PublishNotificationInformation;
import bloomingblooms.domain.notification.delivery.DeliveryNotification;
import bloomingblooms.domain.order.OrderStatusNotification;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import kr.bb.order.dto.request.settlement.SettlementNotification;
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

  @Value("${cloud.aws.sqs.delivery-status-update-notification-queue.url}")
  private String deliveryStatusQueueUrl;

  @Value("${cloud.aws.sqs.settlement-notification-queue.url}")
  private String settlementQueueUrl;

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

  public void publishDeliveryNotification(Long userId, String phoneNumber){
    try{
      DeliveryNotification deliveryNotification =
              DeliveryNotification.builder().userId(userId).phoneNumber(phoneNumber).build();
      PublishNotificationInformation publishNotificationInformation = PublishNotificationInformation.getData(NotificationURL.DELIVERY, NotificationKind.DELIVERY);
      NotificationData<DeliveryNotification> deliveryNotificationNotificationData = NotificationData.notifyData(deliveryNotification, publishNotificationInformation);
      SendMessageRequest sendMessageRequest =
              new SendMessageRequest(deliveryStatusQueueUrl, objectMapper.writeValueAsString(deliveryNotificationNotificationData));
      sqs.sendMessage(sendMessageRequest);
    } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
    }
  }

    public void publishStoreSettlement(List<Long> storeIdList) {
    try {
      SettlementNotification settlementNotification =
          SettlementNotification.builder().storeIdList(storeIdList).build();

      PublishNotificationInformation publishNotificationInformation =
          PublishNotificationInformation.getData(
              NotificationURL.SETTLEMENT, NotificationKind.SETTLEMENT);

      NotificationData<SettlementNotification> settlementNotificationData =
          NotificationData.notifyData(settlementNotification, publishNotificationInformation);

      SendMessageRequest sendMessageRequest =
          new SendMessageRequest(
              settlementQueueUrl, objectMapper.writeValueAsString(settlementNotificationData));

      sqs.sendMessage(sendMessageRequest);

    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Json parse error at publishStoreSettlement!");
    }
  }
}
