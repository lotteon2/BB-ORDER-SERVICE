package kr.bb.order.dto.request.orderForDelivery;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.validation.constraints.NotNull;
import kr.bb.order.entity.OrderDeliveryProduct;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductCreate {
  @NotNull private String productId;
  @NotNull private String productName;
  @NotNull private Long quantity;
  @NotNull private Long price;
  @NotNull private String productThumbnailImage;

  @JsonIgnore
  public long getSumOfEachProduct() {
    return this.price * this.quantity;
  }

  public static OrderDeliveryProduct toEntity(ProductCreate productCreate) {
   return OrderDeliveryProduct.builder()
        .productId(productCreate.getProductId())
        .orderProductPrice(productCreate.getPrice())
        .orderProductQuantity(productCreate.getQuantity())
        .build();
  }
}
