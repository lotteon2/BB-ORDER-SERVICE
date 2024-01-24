package kr.bb.order.util;

import java.util.UUID;

public class OrderUtil {
    public static String generateUUID(){
        return UUID.randomUUID().toString();
    }
}
