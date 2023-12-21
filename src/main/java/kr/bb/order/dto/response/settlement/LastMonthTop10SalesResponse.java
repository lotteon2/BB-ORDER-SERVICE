package kr.bb.order.dto.response.settlement;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class LastMonthTop10SalesResponse {
  List<BestSettlementDto> bestSettlementDtoList;
}
