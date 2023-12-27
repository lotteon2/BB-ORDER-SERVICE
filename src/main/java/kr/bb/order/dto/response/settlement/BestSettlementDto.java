package kr.bb.order.dto.response.settlement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Builder
@AllArgsConstructor
@Getter
public class BestSettlementDto {
  private String name;
  private Integer data;
}
