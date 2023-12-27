package kr.bb.order.mapper;

import bloomingblooms.domain.delivery.DeliveryAddressInsertDto;

public class DeliveryAddressMapper {
  public static DeliveryAddressInsertDto toDto(
      Long deliveryAddressId,
      Long userId,
      String recipientName,
      String zipcode,
      String roadName,
      String addressDetail,
      String phoneNumber) {

    return DeliveryAddressInsertDto.builder()
        .deliveryAddressId(deliveryAddressId)
        .userId(userId)
        .recipientName(recipientName)
        .zipcode(zipcode)
        .roadName(roadName)
        .addressDetail(addressDetail)
        .phoneNumber(phoneNumber)
        .build();
  }
}
