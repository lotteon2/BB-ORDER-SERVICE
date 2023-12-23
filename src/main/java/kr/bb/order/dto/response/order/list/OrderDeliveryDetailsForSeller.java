package kr.bb.order.dto.response.order.list;

import java.util.Map;
import bloomingblooms.domain.product.ProductInformation;
import kr.bb.order.entity.OrderDeliveryProduct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDeliveryDetailsForSeller {
  private Long key;
  private String productId;
  private String thumbnailImage;
  private String name;
  private Long price;
  private Long quantity;
  private Long paymentAmount;

  public static OrderDeliveryDetailsForSeller toDto(
      OrderDeliveryProduct orderDeliveryProduct, Map<String, ProductInformation> productIdMap) {
    ProductInformation productInformation = productIdMap.get(orderDeliveryProduct.getProductId());
    return OrderDeliveryDetailsForSeller.builder()
        .key(orderDeliveryProduct.getOrderProductId())
        .productId(orderDeliveryProduct.getProductId())
        .thumbnailImage(productInformation.getProductThumbnail())
        .name(productInformation.getProductName())
        .price(orderDeliveryProduct.getOrderProductPrice())
        .quantity(orderDeliveryProduct.getOrderProductQuantity())
        .paymentAmount(
            orderDeliveryProduct.getOrderProductPrice()
                * orderDeliveryProduct.getOrderProductQuantity())
        .build();
  }
}
