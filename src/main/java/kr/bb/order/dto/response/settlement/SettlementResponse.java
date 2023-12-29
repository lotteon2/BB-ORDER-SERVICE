package kr.bb.order.dto.response.settlement;


import java.util.List;
import kr.bb.order.dto.feign.StoreInfoDto;
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
  private List<Integer> year;
  private List<Integer> month;
  private List<StoreInfoDto> store;
  private List<SettlementDto> settlementDtoList;
}
