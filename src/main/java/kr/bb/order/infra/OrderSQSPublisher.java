package kr.bb.order.infra;

import bloomingblooms.domain.notification.NotificationData;
import bloomingblooms.domain.notification.NotificationKind;
import bloomingblooms.domain.notification.NotificationURL;
import bloomingblooms.domain.notification.PublishNotificationInformation;
import bloomingblooms.domain.notification.question.InqueryResponseNotification;
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
  @Value("${settlement-notification-queue.url}")
  private String settlementQueueUrl;

  public void publish(Long userId, String phoneNumber) {
    try {
      InqueryResponseNotification inqueryResponseNotification =
          InqueryResponseNotification.builder().userId(userId).phoneNumber(phoneNumber).build();
      PublishNotificationInformation publishNotificationInformation =
          PublishNotificationInformation.getData(
              NotificationURL.ORDER_SUCCESS, NotificationKind.ORDER_SUCCESS);
      NotificationData<InqueryResponseNotification> inqueryResponseNotificationData =
          NotificationData.notifyData(inqueryResponseNotification, publishNotificationInformation);
      SendMessageRequest sendMessageRequest =
          new SendMessageRequest(
              queueUrl, objectMapper.writeValueAsString(inqueryResponseNotificationData));
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
