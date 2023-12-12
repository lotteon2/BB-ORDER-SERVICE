package kr.bb.order.dto.request.orderForDelivery;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.validation.constraints.NotNull;
import kr.bb.order.entity.OrderProduct;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.criterion.Order;

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

  public static OrderProduct toEntity(String orderId, String orderType, ProductCreate productCreate) {
   return OrderProduct.builder()
        .orderId(orderId)
        .orderType(orderType)
        .productId(productCreate.getProductId())
        .orderProductPrice(productCreate.getPrice())
        .orderProductQuantity(productCreate.getQuantity())
        .build();
  }
}
