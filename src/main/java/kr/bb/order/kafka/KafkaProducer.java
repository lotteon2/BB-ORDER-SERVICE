package kr.bb.order.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaProducer<T> {
    private final KafkaTemplate<String, T> kafkaTemplate;

    public void send(String topicName, T data){
      kafkaTemplate.send(topicName, data);
    }
}
