package kr.bb.order.dto.response.settlement;


import java.util.List;
import kr.bb.order.dto.request.store.StoreDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SettlementResponse {
  private Integer totalCnt;
  private Integer year;
  private Integer month;
  private List<StoreDto> store;
  List<SettlementDto> settlementDtoList;
}
