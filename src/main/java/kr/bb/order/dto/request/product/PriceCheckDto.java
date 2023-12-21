package kr.bb.order.dto.request.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceCheckDto {
    private String productId;
    private Long price;

    public static PriceCheckDto toDto(String productId, Long price){
        return PriceCheckDto.builder()
                .productId(productId)
                .price(price)
                .build();
    }
}
