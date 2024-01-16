package kr.bb.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import bloomingblooms.domain.StatusChangeDto;
import bloomingblooms.domain.delivery.DeliveryInfoDto;
import bloomingblooms.domain.notification.delivery.DeliveryStatus;
import bloomingblooms.domain.order.PickupStatusChangeDto;
import bloomingblooms.domain.order.ProcessOrderDto;
import bloomingblooms.response.CommonResponse;
import java.util.HashMap;
import javax.persistence.EntityNotFoundException;
import kr.bb.order.entity.delivery.OrderDelivery;
import kr.bb.order.entity.pickup.OrderPickup;
import kr.bb.order.entity.pickup.OrderPickupStatus;
import kr.bb.order.entity.redis.PickupOrderInfo;
import kr.bb.order.feign.DeliveryServiceClient;
import kr.bb.order.feign.FeignHandler;
import kr.bb.order.infra.OrderSQSPublisher;
import kr.bb.order.kafka.KafkaProducer;
import kr.bb.order.repository.OrderDeliveryRepository;
import kr.bb.order.repository.OrderPickupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class OrderCancelServiceTest {
  @Autowired private OrderCancelService orderCancelService;
  @Autowired private OrderDeliveryRepository orderDeliveryRepository;
  @Autowired private OrderPickupRepository orderPickupRepository;
  @MockBean private FeignHandler feignHandler;
  @MockBean private DeliveryServiceClient deliveryServiceClient;
  @MockBean private SimpleMessageListenerContainer simpleMessageListenerContainer;
  @MockBean private KafkaProducer<ProcessOrderDto> kafkaProducer;
  @MockBean private KafkaProducer<PickupStatusChangeDto> kafkaProducerForOrderQuery;
  @MockBean private OrderSQSPublisher orderSQSPublisher;

  @Test
  @DisplayName("가게 사장과 고객은 가게 배송 주문을 취소할 수 있다")
  void cancelOrder() {
    String orderDeliveryId = "가게주문id";

    DeliveryInfoDto deliveryInfoDto = createDeliveryInfoDto();
    HashMap<Long, DeliveryInfoDto> deliveryInfoDtoHashMap = new HashMap<>();
    deliveryInfoDtoHashMap.put(1L, deliveryInfoDto);

    when(deliveryServiceClient.getDeliveryInfo(any())).thenReturn(CommonResponse.success(deliveryInfoDtoHashMap));
    doNothing().when(feignHandler).cancel(any());
    doNothing().when(kafkaProducer).send(eq("order-create-rollback"), any(ProcessOrderDto.class));
    doNothing().when(orderSQSPublisher).publishOrderCancel(any(), any());

    orderCancelService.cancelOrderDelivery(orderDeliveryId);

    OrderDelivery orderDelivery =
        orderDeliveryRepository.findById(orderDeliveryId).orElseThrow(EntityNotFoundException::new);
    assertThat(orderDelivery.getOrderDeliveryStatus()).isEqualTo(DeliveryStatus.CANCELED);
  }

  @Test
  @DisplayName("가게 사장과 고객은 가게 픽업 주문을 취소할 수 있다")
  void cancelPickupOrder(){
    String orderPickupId = "orderPickupId";

    doNothing().when(feignHandler).cancel(any());
    doNothing().when(kafkaProducer).send(eq("order-create-rollback"), any(ProcessOrderDto.class));
    doNothing().when(kafkaProducerForOrderQuery).send(eq("pickup-status-update"), any(PickupStatusChangeDto.class));
    doNothing().when(orderSQSPublisher).publishOrderCancel(any(), any());

    orderCancelService.cancelOrderPickup(orderPickupId);

    OrderPickup orderPickup = orderPickupRepository.findById(orderPickupId).orElseThrow(EntityNotFoundException::new);
    assertThat(orderPickup.getOrderPickupStatus()).isEqualTo(OrderPickupStatus.CANCELED);

  }


  private DeliveryInfoDto createDeliveryInfoDto() {
    return DeliveryInfoDto.builder()
            .deliveryId(1L)
            .deliveryTrackingNumber("abcd")
            .ordererName("주문자명")
            .ordererPhone("주문자 전화번호")
            .ordererEmail("이메일")
            .recipientPhone("수신자 전화번호")
            .zipcode("우편번호")
            .roadName("도로명주소")
            .addressDetail("상세주소")
            .deliveryRequest("배송요청사항")
            .deliveryCost(2500L)
            .build();
  }

}
