package kr.bb.order.dto.feign;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class StoreInfoDto {

  private Long key;
  private String storeCode;
  private String storeName;
  private String phoneNumber;
  private String bank;
  private String accountNumber;
  private String averageRating;
  private String totalAmount;
  private Date regDate;

}
