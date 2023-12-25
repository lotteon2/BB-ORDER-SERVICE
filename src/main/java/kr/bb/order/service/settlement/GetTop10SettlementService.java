package kr.bb.order.service.settlement;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import kr.bb.order.dto.response.settlement.BestSettlementDto;
import kr.bb.order.dto.response.settlement.LastMonthTop10SalesResponse;
import kr.bb.order.entity.settlement.Settlement;
import kr.bb.order.repository.settlement.SettlementJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class GetTop10SettlementService {

  private final SettlementJpaRepository repository;

    public LastMonthTop10SalesResponse getTop10() {
        List<Settlement> settlementList = repository.findTop10ByOrderBySettlementAmountDesc();

        return convertToSingleDto(settlementList);
    }

    private LastMonthTop10SalesResponse convertToSingleDto(List<Settlement> settlementList) {
        List<BestSettlementDto> bestSettlementDtoList = settlementList.stream()
                .map(this::mapToBestSettlementDto)
                .collect(Collectors.toList());

        return LastMonthTop10SalesResponse.builder()
                .bestSettlementDtoList(bestSettlementDtoList)
                .build();
    }

    private BestSettlementDto mapToBestSettlementDto(Settlement settlement) {
        return BestSettlementDto.builder()
                .name(settlement.getStoreName())
                .data(settlement.getSettlementAmount().intValue())
                .build();
    }

}
