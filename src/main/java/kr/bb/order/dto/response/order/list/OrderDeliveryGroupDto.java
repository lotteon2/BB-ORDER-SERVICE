package kr.bb.order.dto.response.order.list;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import kr.bb.order.dto.request.payment.PaymentInfoDto;
import kr.bb.order.dto.request.product.ProductInfoDto;
import kr.bb.order.entity.delivery.OrderGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDeliveryGroupDto {
    private String key;
    private String orderStatus;
    private String thumbnailImage;
    private String productName;
    private Long quantity;
    private Long paymentAmount;
    private String paymentDate;
    private Long storeCount;

    public static List<OrderDeliveryGroupDto> toDto(List<OrderGroup> orderGroupsList, List<Long> storeCounts, List<String> productIds, Map<String, ProductInfoDto> productInfoDtoMap, List<PaymentInfoDto> paymentInfo){
        List<OrderDeliveryGroupDto> orderDeliveryGroupDtos = new ArrayList<>();

        for(int i=0; i<orderGroupsList.size(); i++){
            OrderDeliveryGroupDto orderDeliveryGroupDto = OrderDeliveryGroupDto.builder()
                    .key(orderGroupsList.get(i).getOrderGroupId())
                    // 한 가게의 주문상태가 해당 그룹주문의 상태가 된다.
                    .orderStatus(orderGroupsList.get(i).getOrderDeliveryList().get(0).getOrderDeliveryStatus().toString())
                    .thumbnailImage( productInfoDtoMap.get(productIds.get(i)).getProductThumbnailImage())
                    .productName(productInfoDtoMap.get(productIds.get(i)).getProductName())
                    .quantity(orderGroupsList.get(i).getOrderDeliveryList().stream()
                            .mapToLong(orderDelivery -> orderDelivery.getOrderDeliveryProducts().size())
                            .sum())
                    .paymentAmount(paymentInfo.get(i).getPaymentActualAmount())
                    .paymentDate(paymentInfo.get(i).getCreatedAt().toLocalDate().toString())
                    .storeCount(storeCounts.get(i))
                    .build();

            orderDeliveryGroupDtos.add(orderDeliveryGroupDto);
        }
        return orderDeliveryGroupDtos;
    }
}
