package kr.bb.order.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void requestOrder(ProcessOrderDto processOrderDto){
        try{
            String jsonString = objectMapper.writeValueAsString(processOrderDto);
            kafkaTemplate.send("coupon-use", jsonString);
        }catch (JsonProcessingException e){
            throw new RuntimeException(e);
        }
    }

    public void rollbackOrder(ProcessOrderDto processOrderDto){
        try{
            String jsonString = objectMapper.writeValueAsString(processOrderDto);
            kafkaTemplate.send("order-create-rollback", jsonString);
        }catch (JsonProcessingException e){
            throw new RuntimeException(e);
        }
    }

    public void deleteFromCart(Map<Long,String> cartCompoKeys){
        try{
            String jsonString = objectMapper.writeValueAsString(cartCompoKeys);
            kafkaTemplate.send("delete-from-cart", jsonString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
