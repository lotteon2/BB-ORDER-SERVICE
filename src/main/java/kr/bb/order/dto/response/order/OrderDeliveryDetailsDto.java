package kr.bb.order.dto.response.order;

import bloomingblooms.domain.card.CardStatus;
import bloomingblooms.domain.review.ReviewStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import bloomingblooms.domain.product.ProductInformation;
import kr.bb.order.entity.OrderDeliveryProduct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDeliveryDetailsDto {
  private Long orderProductId;
  private String productId;
  private String thumbnailImage;
  private String name;
  private Long price;
  private Long quantity;
  private Long totalAmount;
  private ReviewStatus reviewIsWritten;
  private CardStatus cardIsWritten;

  public static List<OrderDeliveryDetailsDto> toDto(
      List<String> productIds, List<ProductInformation> infoDtos, List<OrderDeliveryProduct> orderDeliveryProducts) {
    List<OrderDeliveryDetailsDto> list = new ArrayList<>();

    Map<String, ProductInformation> productInfoDtoMap =
        infoDtos.stream().collect(Collectors.toMap(ProductInformation::getProductId, dto -> dto));
    Map<String, OrderDeliveryProduct> orderProductMap =
        orderDeliveryProducts.stream().collect(Collectors.toMap(OrderDeliveryProduct::getProductId, dto -> dto));

    for (String productId : productIds) {
      list.add(
          OrderDeliveryDetailsDto.builder()
              .orderProductId(orderProductMap.get(productId).getOrderProductId())
              .productId(productId)
              .thumbnailImage(productInfoDtoMap.get(productId).getProductThumbnail())
              .name(productInfoDtoMap.get(productId).getProductName())
              .price(orderProductMap.get(productId).getOrderProductPrice())
              .quantity(orderProductMap.get(productId).getOrderProductQuantity())
              .totalAmount(
                  orderProductMap.get(productId).getOrderProductPrice()
                      * orderProductMap.get(productId).getOrderProductQuantity())
              .reviewIsWritten(orderProductMap.get(productId).getReviewStatus())
              .cardIsWritten(orderProductMap.get(productId).getCardStatus())
              .build());
    }
    return list;
  }
}
