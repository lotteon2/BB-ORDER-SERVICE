package kr.bb.order.infra;

import bloomingblooms.domain.order.NewOrderEvent;
import bloomingblooms.domain.order.NewOrderEvent.NewOrderEventItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSNSPublisher {
  private final SnsClient snsClient;
  private final ObjectMapper objectMapper;

  @Value("${cloud.aws.sns.new-order-event.arn}")
  private String arn;

  public void newOrderEventPublish(List<NewOrderEventItem> newOrderEventList) {
    try {
      PublishResponse publish =
          snsClient.publish(
              PublishRequest.builder()
                  .message(objectMapper.writeValueAsString(newOrderEventList))
                  .topicArn(arn)
                  .build());
      if (publish.sdkHttpResponse().isSuccessful()) {
        log.info("order event success");
      } else log.info("new order event fail");
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
