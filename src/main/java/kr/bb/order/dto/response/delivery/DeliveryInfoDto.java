package kr.bb.order.dto.response.delivery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryInfoDto {
    private String zipcode;
    private String roadName;
    private String addressDetail;
    private String deliveryRequest;
}
