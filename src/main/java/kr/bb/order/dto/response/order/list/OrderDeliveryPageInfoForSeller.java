package kr.bb.order.dto.response.order.list;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDeliveryPageInfoForSeller {
    private Long totalCnt;
    private List<OrderDeliveryInfoForSeller> orders;
    public static OrderDeliveryPageInfoForSeller toDto(Long totalCnt, List<OrderDeliveryInfoForSeller> orders){
        return OrderDeliveryPageInfoForSeller.builder()
                .totalCnt(totalCnt)
                .orders(orders)
                .build();
    }
}
