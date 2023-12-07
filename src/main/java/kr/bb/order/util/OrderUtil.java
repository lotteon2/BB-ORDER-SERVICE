package kr.bb.order.util;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrderUtil {
    public String generateUUID(){
        return UUID.randomUUID().toString();
    }
}
