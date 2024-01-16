package kr.bb.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStatusChangeDto {
  private Long id;  // orderProductId (delivery)
  private String status;
}
