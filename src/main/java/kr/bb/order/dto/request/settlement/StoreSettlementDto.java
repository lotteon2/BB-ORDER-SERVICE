package kr.bb.order.dto.request.settlement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class StoreSettlementDto {

  private Long storeId;
  private Long settlementAmount;

}
