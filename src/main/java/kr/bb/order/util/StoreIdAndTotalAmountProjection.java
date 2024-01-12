package kr.bb.order.util;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Getter
public class StoreIdAndTotalAmountProjection {
  private Long storeId;
  private Long totalAmount;
}