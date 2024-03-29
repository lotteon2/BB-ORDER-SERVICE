package kr.bb.order.feign;

import bloomingblooms.domain.batch.SubscriptionBatchDtoList;
import bloomingblooms.domain.payment.KakaopayApproveRequestDto;
import bloomingblooms.domain.payment.KakaopayReadyRequestDto;
import bloomingblooms.domain.payment.KakaopayReadyResponseDto;
import bloomingblooms.domain.payment.PaymentInfoDto;
import bloomingblooms.domain.payment.PaymentInfoMapDto;
import bloomingblooms.domain.payment.PaymentInfoRequestDto;
import bloomingblooms.response.CommonResponse;
import java.time.LocalDateTime;
import java.util.List;
import kr.bb.order.dto.feign.KakaopayCancelRequestDto;
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
  CommonResponse<LocalDateTime> approve(@RequestBody KakaopayApproveRequestDto approveRequestDto);

  @PostMapping(value = "/client/paymentInfo")
  CommonResponse<List<PaymentInfoDto>> getPaymentInfo(@RequestBody List<String> orderGroupIds);

  @GetMapping(value = "/client/paymentDate")
  CommonResponse<String> getPaymentDate(@RequestParam String orderGroupId);

  @PostMapping(value = "/client/subscription")
  CommonResponse<Void> subscription(@RequestBody SubscriptionBatchDtoList subscriptionBatchDtoList);

  @PostMapping(value = "/client/cancel")
  CommonResponse<Void> cancel(@RequestBody KakaopayCancelRequestDto cancelRequestDto);

  @PostMapping(value = "/client/subscription/cancel")
  CommonResponse<Void> cancelSubscription(@RequestBody KakaopayCancelRequestDto cancelRequestDto);
}
