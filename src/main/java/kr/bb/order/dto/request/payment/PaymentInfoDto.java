package kr.bb.order.dto.request.payment;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInfoDto {
    private String orderId;
    private Long paymentActualAmount;
    private LocalDateTime createdAt;
}
