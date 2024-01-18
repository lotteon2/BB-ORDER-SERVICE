package kr.bb.order.dto.response.settlement;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class LastMonthTop10SalesResponse {
  List<BestSettlementDto> bestSettlementDtoList;
}
