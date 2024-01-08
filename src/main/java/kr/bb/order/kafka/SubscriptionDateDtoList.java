package kr.bb.order.kafka;

import bloomingblooms.domain.subscription.SubscriptionDateDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDateDtoList {
  private List<SubscriptionDateDto> subscriptionDateDtoList;
}
