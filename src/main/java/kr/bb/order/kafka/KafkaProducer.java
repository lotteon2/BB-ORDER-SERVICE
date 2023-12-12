package kr.bb.order.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.bb.order.dto.request.store.ProcessOrderDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendUseCoupon(ProcessOrderDto processOrderDto){
        try{
            String jsonString = objectMapper.writeValueAsString(processOrderDto);
            kafkaTemplate.send("use-coupon", jsonString);
        }catch (JsonProcessingException e){
            throw new RuntimeException(e);
        }
    }
}
