package kr.bb.order.feign;

import bloomingblooms.response.CommonResponse;
import kr.bb.order.dto.request.payment.KakaopayReadyRequestDto;
import kr.bb.order.dto.response.payment.KakaopayReadyResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "paymentServiceClient", url = "${endpoint.payment-service}")
public interface PaymentServiceClient {
  @PostMapping(value = "/payments/ready")
  CommonResponse<KakaopayReadyResponseDto> ready(@RequestBody KakaopayReadyRequestDto readyRequestDto);
}
