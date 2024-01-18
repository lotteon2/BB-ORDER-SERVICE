package kr.bb.order.entity.settlement;

import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import kr.bb.order.entity.common.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Entity
public class Settlement extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long settlementId;
  private String storeName;
  private LocalDateTime settlementDate;
  private Long settlementAmount;
  private String bankName;
  private String accountNumber;
  private Long storeId;
  private Long totalAmountPerMonth;
  private String gugun;
  private String sido;
}
