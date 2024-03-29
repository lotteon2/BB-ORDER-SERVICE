package kr.bb.order.dto.response.order.details;

import bloomingblooms.domain.product.ProductInformation;
import java.util.Map;
import kr.bb.order.entity.OrderDeliveryProduct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRead {
  private Long orderProductId;
  private String productId;
  private String thumbnailImage;
  private String name;
  private Long price;
  private Long quantity;
  private Long totalAmount;
  private String reviewStatus;
  private String cardStatus;

  public static ProductRead toDto(
      OrderDeliveryProduct orderDeliveryProduct, Map<String, ProductInformation> productInfoDtoMap) {
    ProductInformation productInformation = productInfoDtoMap.get(orderDeliveryProduct.getProductId());

    return ProductRead.builder()
        .orderProductId(orderDeliveryProduct.getOrderProductId())
        .productId(orderDeliveryProduct.getProductId())
        .thumbnailImage(productInformation.getProductThumbnail())
        .name(productInformation.getProductName())
        .price(orderDeliveryProduct.getOrderProductPrice())
        .quantity(orderDeliveryProduct.getOrderProductQuantity())
        .totalAmount(
            orderDeliveryProduct.getOrderProductPrice()
                * orderDeliveryProduct.getOrderProductQuantity())
        .reviewStatus(orderDeliveryProduct.getReviewStatus().toString())
        .cardStatus(orderDeliveryProduct.getCardStatus().toString())
        .build();
  }
}
