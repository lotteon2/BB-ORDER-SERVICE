package kr.bb.order.dto.response.order;

import java.util.List;
import jdk.jfr.Name;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDeliveryInfoForSeller {
    private String orderGroupId;
    private String orderDeliveryId;
    private List<OrderDeliveryDetailsForSeller> products;
    private String orderDeliveryStatus;
    private Long paymentAmount;
    private Long couponAmount;
    private String paymentDate;
    private String zipcode;
    private String roadName;
    private String addressDetail;
    private String deliveryRequest;
}
