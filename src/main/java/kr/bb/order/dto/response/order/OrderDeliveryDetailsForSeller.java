package kr.bb.order.dto.response.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDeliveryDetailsForSeller {
    private Long orderDeliveryId;
    private Long orderProductId;
    private String thumbnailImage;
    private String name;
    private Long price;
    private Long quantity;
    private Long paymentAmount;
}
