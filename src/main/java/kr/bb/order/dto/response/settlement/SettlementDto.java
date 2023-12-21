package kr.bb.order.dto.response.settlement;

import java.time.LocalDateTime;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SettlementDto {

  private Long key;
  private String storeName;
  private LocalDateTime settlementDate;
  private Long settlementAmount;
  private String bankName;
  private String accountNumber;

}
