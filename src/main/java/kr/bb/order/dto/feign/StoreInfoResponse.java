package kr.bb.order.dto.feign;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Getter
@NoArgsConstructor
public class StoreInfoResponse {
  private Integer totalCnt;
  private StoreInfoDto storeInfoDto;
}
