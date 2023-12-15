package kr.bb.order.dto.response.order;

import java.util.ArrayList;
import java.util.List;
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

    public static List<OrderDeliveryGroupDto> toDto(List<OrderGroup> orderGroupsList, List<Long> storeCounts, List<ProductInfoDto> productInfo, List<PaymentInfoDto> paymentInfo){
        List<OrderDeliveryGroupDto> orderDeliveryGroupDtos = new ArrayList<>();

        for(int i=0; i<orderGroupsList.size(); i++){
            OrderDeliveryGroupDto orderDeliveryGroupDto = OrderDeliveryGroupDto.builder()
                    .key(orderGroupsList.get(i).getOrderGroupId())
                    .orderStatus(orderGroupsList.get(i).getOrderDeliveryList().get(i).getOrderDeliveryStatus().toString())
                    .thumbnailImage(productInfo.get(i).getProductThumbnailImage())
                    .productName(productInfo.get(i).getProductName())
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
