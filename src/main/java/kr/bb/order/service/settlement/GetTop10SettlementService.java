package kr.bb.order.service.settlement;

import java.util.List;
import kr.bb.order.dto.response.settlement.LastMonthTop10SalesResponse;
import kr.bb.order.entity.settlement.Settlement;
import kr.bb.order.repository.settlement.SettlementJpaRepository;
import kr.bb.order.util.SettlementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class GetTop10SettlementService {

  private final SettlementJpaRepository repository;

    public LastMonthTop10SalesResponse getTop10() {
      List<Settlement> settlementList = repository.findTop10ByOrderBySettlementAmountDesc(
          PageRequest.of(0, 10));
      return SettlementMapper.settlementListToBestTop10Dto(settlementList);
    }





}
