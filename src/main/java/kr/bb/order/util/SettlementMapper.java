package kr.bb.order.util;

import java.util.List;
import java.util.stream.Collectors;
import kr.bb.order.dto.response.settlement.BestSettlementDto;
import kr.bb.order.dto.response.settlement.LastMonthTop10SalesResponse;
import kr.bb.order.dto.response.settlement.SettlementDto;
import kr.bb.order.entity.settlement.Settlement;
import org.springframework.data.domain.Page;

public class SettlementMapper {


  private SettlementMapper(){

  }
  public static List<SettlementDto> pageSettlementToDtoList(Page<Settlement> settlements) {
    return settlements.map(settlement -> SettlementDto.builder()
            .key(settlement.getSettlementId())
            .storeName(settlement.getStoreName())
            .settlementDate(settlement.getSettlementDate())
            .settlementAmount(settlement.getSettlementAmount())
            .bankName(settlement.getBankName())
            .accountNumber(settlement.getAccountNumber())
            .build())
        .toList();
  }

    private static BestSettlementDto mapToBestSettlementDto(Settlement settlement) {
    return BestSettlementDto.builder()
        .name(settlement.getStoreName())
        .data(settlement.getSettlementAmount().intValue())
        .build();
  }

  public static LastMonthTop10SalesResponse settlementListToBestTop10Dto(List<Settlement> settlementList) {
    List<BestSettlementDto> bestSettlementDtoList = settlementList.stream()
        .map(SettlementMapper::mapToBestSettlementDto)
        .collect(Collectors.toList());

    return LastMonthTop10SalesResponse.builder()
        .bestSettlementDtoList(bestSettlementDtoList)
        .build();
  }
}
