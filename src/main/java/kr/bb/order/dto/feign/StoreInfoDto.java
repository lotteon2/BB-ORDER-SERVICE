package kr.bb.order.dto.feign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class StoreInfoDto {

  private Long value;
  private String label;

}
