package kr.bb.order.dto.request.orderForDelivery;

import bloomingblooms.domain.order.ProductCreate;
import kr.bb.order.entity.OrderDeliveryProduct;

public class ProductCreateManager {
  public static OrderDeliveryProduct toEntity(ProductCreate productCreate) {
    return OrderDeliveryProduct.builder()
        .productId(productCreate.getProductId())
        .orderProductPrice(productCreate.getPrice())
        .orderProductQuantity(productCreate.getQuantity())
        .build();
  }
}
