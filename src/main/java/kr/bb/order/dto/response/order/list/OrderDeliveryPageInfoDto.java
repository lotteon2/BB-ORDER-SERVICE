package kr.bb.order.dto.response.order.list;

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
    private List<OrderDeliveryGroupDto> orders;

    public static OrderDeliveryPageInfoDto toDto(Long totalCnt, List<OrderDeliveryGroupDto> orders){
        return OrderDeliveryPageInfoDto.builder()
                .totalCnt(totalCnt)
                .orders(orders)
                .build();
    }
}
