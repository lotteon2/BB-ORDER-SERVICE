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
// TODO: MVN REPO 로 빼기
public class OrderSubscriptionBatchDto {
  private List<String> orderSubscriptionIds;
}
