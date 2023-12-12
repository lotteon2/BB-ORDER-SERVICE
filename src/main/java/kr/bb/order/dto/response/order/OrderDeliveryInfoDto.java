package kr.bb.order.dto.response.order;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kr.bb.order.entity.OrderProduct;
import kr.bb.order.entity.delivery.OrderDelivery;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDeliveryInfoDto {
    private String orderDeliveryId;
    private String orderStatus;
    private List<OrderDeliveryDetailsDto> products;

    public static OrderDeliveryInfoDto toDto(String orderDeliveryId, List<OrderDelivery> orderDeliveries, List<OrderProduct> orderProducts, List<OrderDeliveryDetailsDto> orderDeliveryDetailDtos){
        Map<String, OrderDelivery> orderDeliveryMap = orderDeliveries.stream().collect(Collectors.toMap(OrderDelivery::getOrderDeliveryId, dto->dto));

        List<Long> orderProductIds = orderProducts.stream().map(OrderProduct::getOrderProductId).collect(Collectors.toList());

        List<OrderDeliveryDetailsDto> filteredDtoOrderDeliveryDetails = orderDeliveryDetailDtos.stream().filter(odd -> orderProductIds.contains(odd.getOrderProductId()))
                .collect(Collectors.toList());

        return OrderDeliveryInfoDto.builder()
                .orderDeliveryId(orderDeliveryId)
                .orderStatus(orderDeliveryMap.get(orderDeliveryId).getOrderDeliveryStatus().toString())
                .products(filteredDtoOrderDeliveryDetails)
                .build();
    }
}
