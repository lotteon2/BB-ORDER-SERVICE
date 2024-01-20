package kr.bb.order.dto.request;

import bloomingblooms.domain.payment.PaymentInfoDto;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInfoMapDto {
  Map<String, PaymentInfoDto> paymentInfoDtoMap;
}
