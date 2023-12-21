package kr.bb.order.service;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import kr.bb.order.dto.response.settlement.LastMonthTop10SalesResponse;
import kr.bb.order.dto.response.settlement.SettlementDto;
import kr.bb.order.entity.settlement.Settlement;
import kr.bb.order.repository.settlement.SettlementJpaRepository;
import kr.bb.order.repository.settlement.SettlementSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class SettlementService {

  private final SettlementJpaRepository repository;

  private Page<Settlement> getFilteredSettlements(Long storeId, Integer year, Integer month,
      int page, int size) {
    PageRequest pageRequest = PageRequest.of(page, size);
    Specification<Settlement> specification = SettlementSpecification.filterSettlements(storeId,
        year, month);
    return repository.findAll(specification, pageRequest);
  }

  public List<SettlementDto> getSettlement(Long storeId, Integer year, Integer month, int page,
      int size) {
    Page<Settlement> settlementPage = getFilteredSettlements(storeId, year, month, page, size);
    return convertToDto(settlementPage);
  }

  public List<LastMonthTop10SalesResponse> getTop10() {
    Pageable pageable = PageRequest.of(0, 10);
    List<Settlement> settlementList = repository.findTop10ByOrderBySettlementAmountDesc(pageable);
    return ;
  }


  private List<LastMonthTop10SalesResponse> convertToDto(List<Settlement> settlementList){
  }

  private List<SettlementDto> convertToDto(Page<Settlement> settlements) {
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


}
