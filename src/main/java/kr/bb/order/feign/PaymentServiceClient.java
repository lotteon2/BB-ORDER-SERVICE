package kr.bb.order.feign;

import bloomingblooms.response.CommonResponse;
import java.util.List;
import kr.bb.order.dto.request.payment.PaymentInfoDto;
import kr.bb.order.dto.request.payment.KakaopayApproveRequestDto;
import kr.bb.order.dto.request.payment.KakaopayReadyRequestDto;
import kr.bb.order.dto.response.payment.KakaopayReadyResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "paymentServiceClient", url = "${endpoint.payment-service}")
public interface PaymentServiceClient {
  @PostMapping(value = "/client/ready")
  CommonResponse<KakaopayReadyResponseDto> ready(@RequestBody KakaopayReadyRequestDto readyRequestDto);

  @PostMapping(value = "/client/approve")
  CommonResponse<Void> approve(@RequestBody KakaopayApproveRequestDto approveRequestDto);

  @GetMapping(value = "/client/paymentInfo")
  CommonResponse<List<PaymentInfoDto>> getPaymentInfo(@RequestParam List<String> orderGroupIds);

  @GetMapping(value = "/client/paymentDate")
  CommonResponse<String> getPaymentDate(@RequestParam String orderGroupId);
}
