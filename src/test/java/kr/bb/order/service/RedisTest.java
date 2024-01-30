package kr.bb.order.service;

import static org.assertj.core.api.Assertions.assertThat;

import bloomingblooms.domain.notification.order.OrderType;
import bloomingblooms.domain.order.OrderMethod;
import kr.bb.order.entity.redis.OrderInfo;
import kr.bb.order.entity.redis.PickupOrderInfo;
import kr.bb.order.entity.redis.SubscriptionOrderInfo;
import kr.bb.order.util.RedisOperation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;

@SpringBootTest
public class RedisTest {
  @Autowired RedisOperation redisOperation;
  @MockBean private SimpleMessageListenerContainer simpleMessageListenerContainer;

  @Test
  @DisplayName("redis에 여러 데이터 타입을 저장하거나 불러올 수 있다")
  void redisTest() {
    String orderId = "임시orderId";
    OrderInfo orderInfo =
        OrderServiceTest.createOrderInfo(orderId, OrderType.SUBSCRIBE, OrderMethod.DIRECT);
    redisOperation.saveIntoRedis(orderId, orderInfo);

    String orderPickupId = "임시pickupId";
    PickupOrderInfo pickupOrderInfo = OrderServiceTest.createPickupOrderInfo(orderPickupId);
    redisOperation.saveIntoRedis(orderPickupId, pickupOrderInfo);

    String orderSubscriptionId = "임시subscriptionId";
    SubscriptionOrderInfo subscriptionOrderInfo =
        OrderServiceTest.createSubscriptionOrderInfo(orderSubscriptionId);
    redisOperation.saveIntoRedis(orderSubscriptionId, subscriptionOrderInfo);

    assertThat(redisOperation.findFromRedis(orderId, OrderInfo.class).getTempOrderId())
        .isEqualTo(orderId);
    assertThat(redisOperation.findFromRedis(orderPickupId, PickupOrderInfo.class).getTempOrderId())
        .isEqualTo(orderPickupId);
    assertThat(
            redisOperation
                .findFromRedis(orderSubscriptionId, SubscriptionOrderInfo.class)
                .getTempOrderId())
        .isEqualTo(orderSubscriptionId);
  }
}
