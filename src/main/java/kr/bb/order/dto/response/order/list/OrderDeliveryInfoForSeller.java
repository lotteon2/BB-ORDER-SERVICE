package kr.bb.order.dto.response.order.list;

import bloomingblooms.domain.payment.PaymentInfoDto;
import java.util.List;
import java.util.Map;
import bloomingblooms.domain.delivery.DeliveryInfoDto;
import kr.bb.order.entity.delivery.OrderDelivery;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDeliveryInfoForSeller {
    private String key; // orderGroupId
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

    public static OrderDeliveryInfoForSeller toDto(OrderDelivery orderDelivery, List<OrderDeliveryDetailsForSeller> detailsList, Map<String, PaymentInfoDto> paymentInfoMap,  Map<Long, DeliveryInfoDto> deliveryInfoMap){
        PaymentInfoDto paymentInfo = paymentInfoMap.get(
                orderDelivery.getOrderGroup().getOrderGroupId());
        DeliveryInfoDto deliveryInfo = deliveryInfoMap.get(orderDelivery.getDeliveryId());

        return OrderDeliveryInfoForSeller.builder()
                .key(orderDelivery.getOrderGroup().getOrderGroupId())
                .orderDeliveryId(orderDelivery.getOrderDeliveryId())
                .products(detailsList)
                .orderDeliveryStatus(orderDelivery.getOrderDeliveryStatus().toString())
                .paymentAmount(paymentInfo.getPaymentActualAmount())
                .couponAmount(orderDelivery.getOrderDeliveryCouponAmount())
                .paymentDate(paymentInfo.getCreatedAt().toLocalDate().toString())
                .zipcode(deliveryInfo.getZipcode())
                .roadName(deliveryInfo.getRoadName())
                .addressDetail(deliveryInfo.getAddressDetail())
                .deliveryRequest(deliveryInfo.getDeliveryRequest())
                .build();
    }
}
