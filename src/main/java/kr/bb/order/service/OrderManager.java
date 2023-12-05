package kr.bb.order.service;

import java.util.List;
import kr.bb.order.dto.request.orderForDelivery.OrderInfoByStore;
import kr.bb.order.dto.request.orderForDelivery.ProductCreate;
import kr.bb.order.exception.InvalidOrderAmountException;
import org.springframework.stereotype.Component;

@Component
public class OrderManager {
  public void checkActualAmountIsValid(
      List<OrderInfoByStore> orderInfoByStores, Long sumOfActualAmount) {
    long calculatedAmount = 0;
    for (OrderInfoByStore orderInfoByStore : orderInfoByStores) {
      long sumOfEachStore = 0;
      // 상품별 총 가격
      for (int j = 0; j < orderInfoByStore.getProducts().size(); j++) {
        ProductCreate productCreate = orderInfoByStore.getProducts().get(j);
        long sumOfEachProduct = productCreate.getPrice() * productCreate.getQuantity();
        sumOfEachStore += sumOfEachProduct;
      }
      sumOfEachStore =
          sumOfEachStore - orderInfoByStore.getCouponAmount() + orderInfoByStore.getDeliveryCost();
      calculatedAmount += sumOfEachStore;
    }

    if (calculatedAmount == sumOfActualAmount) return;
    else throw new InvalidOrderAmountException();
  }
}
