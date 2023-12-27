package kr.bb.order.dto.response.settlement;


import bloomingblooms.domain.store.StoreInfoDto;
import java.util.List;
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
  private List<StoreInfoDto> store;
  List<SettlementDto> settlementDtoList;
}
