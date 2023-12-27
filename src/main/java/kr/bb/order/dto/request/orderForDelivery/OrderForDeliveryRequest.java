package kr.bb.order.dto.request.orderForDelivery;

import bloomingblooms.domain.order.OrderInfoByStore;
import java.util.List;
import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderForDeliveryRequest {
  @NotEmpty private List<OrderInfoByStore> orderInfoByStores;
  @NotEmpty private Long sumOfActualAmount;
  @Nullable private Long deliveryAddressId;
  @NotEmpty private String ordererName;
  @NotEmpty private String ordererPhoneNumber;
  @NotEmpty private String ordererEmail;
  @NotEmpty private String recipientName;
  @NotEmpty private String deliveryZipcode;
  @NotEmpty private String deliveryRoadName;
  @NotEmpty private String deliveryAddressDetail;
  @NotEmpty private String recipientPhone;
  @NotEmpty private String deliveryRequest;
}
