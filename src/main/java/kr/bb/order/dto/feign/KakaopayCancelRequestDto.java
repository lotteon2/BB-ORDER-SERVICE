package kr.bb.order.dto.feign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KakaopayCancelRequestDto {
    private String orderGroupId;
    private Long cancelAmount;
}
