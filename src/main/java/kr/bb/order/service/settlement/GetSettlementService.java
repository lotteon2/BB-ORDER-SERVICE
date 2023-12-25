package kr.bb.order.service.settlement;


import java.util.List;
import kr.bb.order.dto.response.settlement.SettlementDto;
import kr.bb.order.entity.settlement.Settlement;
import kr.bb.order.repository.settlement.SettlementJpaRepository;
import kr.bb.order.repository.settlement.SettlementSpecification;
import kr.bb.order.util.SettlementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetSettlementService {

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
    return SettlementMapper.pageSettlementToDtoList(settlementPage);
  }



}
