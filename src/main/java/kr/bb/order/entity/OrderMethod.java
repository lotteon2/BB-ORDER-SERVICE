package kr.bb.order.entity;

import lombok.Getter;

@Getter
public enum OrderMethod {
    CART("카트"),
    DIRECT("바로 주문");

    private final String orderMethod;

    OrderMethod(String orderMethod){
        this.orderMethod = orderMethod;
    }
}
