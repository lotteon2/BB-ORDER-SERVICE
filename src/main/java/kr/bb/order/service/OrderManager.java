package kr.bb.order.service;

import bloomingblooms.domain.order.OrderInfoByStore;
import bloomingblooms.domain.order.ProductCreate;
import java.util.List;
import kr.bb.order.exception.InvalidOrderAmountException;
import org.springframework.stereotype.Component;

@Component
public class OrderManager {
    public void checkActualAmountIsValid(List<OrderInfoByStore> orderInfoByStores, Long sumOfActualAmount){
        long calculatedAmount = 0;
        for (OrderInfoByStore orderInfoByStore : orderInfoByStores) {
            long sumOfEachStore = 0;
            // 상품별 총 가격
            for (ProductCreate productCreate :orderInfoByStore.getProducts() ) {
                long sumOfEachProduct = productCreate.getSumOfEachProduct();
                sumOfEachStore += sumOfEachProduct;
            }
            sumOfEachStore -= orderInfoByStore.getCouponAmount();
            sumOfEachStore += orderInfoByStore.getDeliveryCost();
            calculatedAmount += sumOfEachStore;
        }

        if(calculatedAmount != sumOfActualAmount) throw new InvalidOrderAmountException();
    }
}
