package kr.bb.order.util;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import kr.bb.order.dto.feign.StoreInfoDto;
import kr.bb.order.dto.response.settlement.BestSettlementDto;
import kr.bb.order.dto.response.settlement.LastMonthTop10SalesResponse;
import kr.bb.order.dto.response.settlement.SettlementDto;
import kr.bb.order.dto.response.settlement.SettlementResponse;
import kr.bb.order.entity.settlement.Settlement;

public class SettlementMapper {


  private SettlementMapper() {

  }

  private static List<SettlementDto> settlementDtoListFromSettlementList(
      List<Settlement> settlements) {
    return settlements.stream().map(settlement -> SettlementDto.builder()
        .key(settlement.getSettlementId())
        .storeName(settlement.getStoreName())
        .settlementDate(settlement.getSettlementDate())
        .settlementAmount(settlement.getSettlementAmount())
        .totalAmountPerMonth((long) (settlement.getSettlementAmount() * 0.93))
        .bankName(settlement.getBankName())
        .accountNumber(settlement.getAccountNumber())
        .build()).collect(Collectors.toList());
  }

private static List<StoreInfoDto> getStoreInfoDtoBySettlement(List<Settlement> settlements) {
    List<StoreInfoDto> sortedList = new ArrayList<>(settlements.stream()
            .map(settlement -> StoreInfoDto.builder()
                    .value(settlement.getStoreId())
                    .label(settlement.getStoreName())
                    .build())
            .collect(Collectors.toList()));

    Collator collator = Collator.getInstance(Locale.KOREAN);
    sortedList.sort((a, b) -> {
        String labelA = a.getLabel().replaceAll("\\n", "");
        String labelB = b.getLabel().replaceAll("\\n", "");

        if (isKorean(labelA) && isKorean(labelB)) {
            return collator.compare(labelA, labelB);
        } else if (isKorean(labelA)) {
            return -1;
        } else if (isKorean(labelB)) {
            return 1;
        } else {
            return labelA.compareTo(labelB);
        }
    });

    return sortedList;
}

private static boolean isKorean(String text) {
    return text.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*");
}

 private static List<Integer> getUniqueYears(List<Settlement> settlements) {
    return settlements.stream()
            .map(settlement -> settlement.getSettlementDate().getYear())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
}

private static List<Integer> getUniqueMonths(List<Settlement> settlements) {
    return settlements.stream()
            .map(settlement -> settlement.getSettlementDate().getMonthValue())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
}

  public static SettlementResponse aggregateSettlements(List<Settlement> settlements) {
    List<SettlementDto> settlementDtoList = settlementDtoListFromSettlementList(settlements);
    List<Integer> uniqueYears = getUniqueYears(settlements);
    List<Integer> uniqueMonths = getUniqueMonths(settlements);
    int totalCnt = settlements.size();
    List<StoreInfoDto> storeInfoDto = getStoreInfoDtoBySettlement(settlements);

    return SettlementResponse.builder()
        .settlementDtoList(settlementDtoList)
        .year(uniqueYears)
        .month(uniqueMonths)
        .totalCnt(totalCnt)
        .store(storeInfoDto)
        .build();
  }

  private static BestSettlementDto mapToBestSettlementDto(Settlement settlement) {
    return BestSettlementDto.builder()
        .name(settlement.getStoreName())
        .data(List.of(settlement.getSettlementAmount().intValue()))
        .build();
  }

  public static LastMonthTop10SalesResponse settlementListToBestTop10Dto(
      List<Settlement> settlementList) {
    List<BestSettlementDto> bestSettlementDtoList = settlementList.stream()
        .map(SettlementMapper::mapToBestSettlementDto)
        .collect(Collectors.toList());

    return LastMonthTop10SalesResponse.builder()
        .bestSettlementDtoList(bestSettlementDtoList)
        .build();
  }
}
