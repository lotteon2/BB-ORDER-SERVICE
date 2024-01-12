package kr.bb.order.dto.request.settlement;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class UpdateSettlementCommand {
  List<StoreSettlementDto> dtoList;

}

