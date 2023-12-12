package kr.bb.order.dto.response.order;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDeliveryPageInfoDto {
    private Long totalCnt;
    private List<OrderDeliveryGroupInfoDto> orders;
}
