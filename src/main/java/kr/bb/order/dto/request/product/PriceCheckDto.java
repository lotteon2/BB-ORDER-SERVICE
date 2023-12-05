package kr.bb.order.dto.request.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceCheckDto {
    private Long productId;
    private Long price;

    public static PriceCheckDto toDto(Long productId, Long price){
        return PriceCheckDto.builder()
                .productId(productId)
                .price(price)
                .build();
    }
}
