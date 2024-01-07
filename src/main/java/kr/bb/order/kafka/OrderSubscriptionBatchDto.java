package kr.bb.order.kafka;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSubscriptionBatchDto {
  private List<String> orderSubscriptionIds;
}
