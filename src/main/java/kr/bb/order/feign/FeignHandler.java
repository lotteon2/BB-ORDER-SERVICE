package kr.bb.order.feign;

import bloomingblooms.domain.batch.SubscriptionBatchDtoList;
import bloomingblooms.domain.delivery.DeliveryAddressInsertDto;
import bloomingblooms.domain.delivery.DeliveryInsertDto;
import bloomingblooms.domain.order.ValidatePolicyDto;
import bloomingblooms.domain.payment.KakaopayApproveRequestDto;
import bloomingblooms.domain.payment.KakaopayReadyRequestDto;
import bloomingblooms.domain.payment.KakaopayReadyResponseDto;
import bloomingblooms.domain.product.IsProductPriceValid;
import bloomingblooms.response.CommonResponse;
import java.time.LocalDateTime;
import java.util.List;
import kr.bb.order.dto.feign.KakaopayCancelRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeignHandler {
  private final ProductServiceClient productServiceClient;
  private final StoreServiceClient storeServiceClient;
  private final DeliveryServiceClient deliveryServiceClient;
  private final PaymentServiceClient paymentServiceClient;
  private final UserServiceClient userServiceClient;

  /*
   FEIGN 통신
  */
  public void validatePrice(List<IsProductPriceValid> priceCheckDtos) {
    CommonResponse<Void> productCommonResponse = productServiceClient.validatePrice(priceCheckDtos);
    if (productCommonResponse.getResult() == CommonResponse.Result.FAIL) {
      throw new RuntimeException(productCommonResponse.getMessage());
    }
  }

  public void validatePurchaseDetails(ValidatePolicyDto validatePolicyDto) {
    CommonResponse<Void> storeCommonResponse =
        storeServiceClient.validatePurchaseDetails(validatePolicyDto);
    if (storeCommonResponse.getResult() == CommonResponse.Result.FAIL) {
      throw new RuntimeException(storeCommonResponse.getMessage());
    }
  }

  public void createDeliveryAddress(DeliveryAddressInsertDto deliveryAddressInsertDto) {
    CommonResponse<Void> deliveryCommonResponse =
        deliveryServiceClient.createDeliveryAddress(deliveryAddressInsertDto);
    if (deliveryCommonResponse.getResult() == CommonResponse.Result.FAIL) {
      throw new RuntimeException(deliveryCommonResponse.getMessage());
    }
  }

  public KakaopayReadyResponseDto ready(KakaopayReadyRequestDto readyRequestDto) {
    CommonResponse<KakaopayReadyResponseDto> paymentCommonResponse =
        paymentServiceClient.ready(readyRequestDto);
    if (paymentCommonResponse.getResult() == CommonResponse.Result.FAIL) {
      throw new RuntimeException(paymentCommonResponse.getMessage());
    }
    return paymentCommonResponse.getData();
  }

  public List<Long> createDelivery(List<DeliveryInsertDto> dtoList) {
    CommonResponse<List<Long>> deliveryCommonResponse =
        deliveryServiceClient.createDelivery(dtoList);
    if (deliveryCommonResponse.getResult() == CommonResponse.Result.FAIL) {
      throw new RuntimeException(deliveryCommonResponse.getMessage());
    }
    return deliveryCommonResponse.getData();
  }

  public LocalDateTime approve(KakaopayApproveRequestDto requestDto) {
    CommonResponse<LocalDateTime> paymentCommonResponse = paymentServiceClient.approve(requestDto);
    if (paymentCommonResponse.getResult() == CommonResponse.Result.FAIL) {
      throw new RuntimeException(paymentCommonResponse.getMessage());
    }
    return paymentCommonResponse.getData();
  }

  public void processSubscription(SubscriptionBatchDtoList subscriptionBatchDtoList) {
    CommonResponse<Void> paymentCommonResponse =
        paymentServiceClient.subscription(subscriptionBatchDtoList);
    if (paymentCommonResponse.getResult() == CommonResponse.Result.FAIL) {
      throw new RuntimeException(paymentCommonResponse.getMessage());
    }
  }

  public void cancel(KakaopayCancelRequestDto requestDto) {
    CommonResponse<Void> paymentCommonResponse = paymentServiceClient.cancel(requestDto);
    if (paymentCommonResponse.getResult() == CommonResponse.Result.FAIL) {
      throw new RuntimeException(paymentCommonResponse.getMessage());
    }
  }

  public void cancelSubscription(KakaopayCancelRequestDto requestDto){
    CommonResponse<Void> paymentCommonResponse = paymentServiceClient.cancelSubscription(requestDto);
    if(paymentCommonResponse.getResult() == CommonResponse.Result.FAIL){
      throw new RuntimeException(paymentCommonResponse.getMessage());
    }
  }
}
