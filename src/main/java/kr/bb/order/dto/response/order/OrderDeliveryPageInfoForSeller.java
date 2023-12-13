package kr.bb.order.dto.response.order;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDeliveryPageInfoForSeller {
    private Long totalCnt;
    private List<OrderDeliveryInfoForSeller> orders;
}
