package kr.bb.order.controller.restcontroller;

import bloomingblooms.response.CommonResponse;
import java.util.List;
import kr.bb.order.controller.helper.GetStoreInfoFeignRequestFacade;
import kr.bb.order.dto.request.store.StoreDto;
import kr.bb.order.dto.response.settlement.SettlementDto;
import kr.bb.order.dto.response.settlement.SettlementResponse;
import kr.bb.order.dto.response.settlement.LastMonthTop10SalesResponse;
import kr.bb.order.service.settlement.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class SettlementRestController {

  private final SettlementService settlementService;
  private final GetStoreInfoFeignRequestFacade storeInfoFeignRequest;

  @GetMapping("/admin/settlement")
  public CommonResponse<SettlementResponse> getSettlements(
      @RequestParam int year,
      @RequestParam int month,
      @RequestParam(required = false) Long storeId,
      Pageable pageable) {

    List<SettlementDto> settlementDtoList = settlementService.getSettlement(storeId, year, month,
        pageable.getPageSize(),
        pageable.getPageSize());

    List<StoreDto> storeDtoList = storeInfoFeignRequest.handleFeign(storeId);

    return CommonResponse.success(
        SettlementResponse.builder().totalCnt(settlementDtoList.size()).month(month).year(year)
            .store(storeDtoList).settlementDtoList(settlementDtoList).build());

  }


  @GetMapping("/admin/sales")
  public CommonResponse<LastMonthTop10SalesResponse> getSalesTop10() {
    return CommonResponse.success(settlementService.getTop10());
  }


}
