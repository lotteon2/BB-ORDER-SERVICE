package kr.bb.order.dto.request.orderForDelivery;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreate {
  @NotNull private Long productId;
  @NotNull private String productName;
  @NotNull private Long quantity;
  @NotNull private Long price;
}
