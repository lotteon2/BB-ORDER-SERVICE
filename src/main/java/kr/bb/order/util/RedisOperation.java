package kr.bb.order.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisOperation {
  private final ObjectMapper objectMapper;
  private final RedisTemplate<String, String> redisTemplate;

  public <T> T findFromRedis(String id, Class<T> type) {
    try {
      String jsonString = redisTemplate.opsForValue().get(id);
      return objectMapper.readValue(jsonString, type);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public void saveIntoRedis(String tempOrderId, Object object) {
    try {
      String jsonString = objectMapper.writeValueAsString(object);
      redisTemplate.opsForValue().set(tempOrderId, jsonString);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public void expire(String orderId, int time, TimeUnit timeUnit) {
    redisTemplate.expire(orderId, time, timeUnit);
  }
}
