package kr.bb.order.dto.response.settlement;

import java.util.List;
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
  private List<Integer> data;
}
