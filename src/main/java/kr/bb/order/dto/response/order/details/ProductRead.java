package kr.bb.order.dto.response.order.details;

import java.util.Map;
import kr.bb.order.dto.request.product.ProductInfoDto;
import kr.bb.order.entity.OrderDeliveryProduct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

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
      OrderDeliveryProduct orderDeliveryProduct, Map<String, ProductInfoDto> productInfoDtoMap) {
    ProductInfoDto productInfoDto = productInfoDtoMap.get(orderDeliveryProduct.getProductId());

    return ProductRead.builder()
        .orderProductId(orderDeliveryProduct.getOrderProductId())
        .productId(orderDeliveryProduct.getProductId())
        .thumbnailImage(productInfoDto.getProductThumbnailImage())
        .name(productInfoDto.getProductName())
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
