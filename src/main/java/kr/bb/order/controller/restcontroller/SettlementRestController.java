package kr.bb.order.controller.restcontroller;

import bloomingblooms.response.CommonResponse;
import kr.bb.order.dto.response.settlement.LastMonthTop10SalesResponse;
import kr.bb.order.dto.response.settlement.SettlementResponse;
import kr.bb.order.service.settlement.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
@RestController
public class SettlementRestController {

  private final SettlementService settlementService;

  @GetMapping("/admin/settlement")
  public CommonResponse<SettlementResponse> getSettlements(
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) Integer month,
      @RequestParam(required = false) Long storeId,
      @RequestParam(required = false) String sido,
      @RequestParam(required = false) String gugun,
      Pageable pageable) {

      SettlementResponse response = settlementService.getSettlement(sido, gugun,storeId,
        year, month,
        pageable.getPageNumber(),
        pageable.getPageSize());


    return CommonResponse.success(response);

  }


  @GetMapping("/store/settlement")
  public CommonResponse<SettlementResponse> getSettlements(
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) Integer month,
      @RequestParam(required = false) Long storeId,
      Pageable pageable) {

    SettlementResponse response = settlementService.getSettlementWithoutLocation(storeId,
        year, month,
        pageable.getPageNumber(),
        pageable.getPageSize());
    
    return CommonResponse.success(response);

  }


  @GetMapping("/admin/sales")
  public CommonResponse<LastMonthTop10SalesResponse> getSalesTop10() {
    return CommonResponse.success(settlementService.getTop10());
  }


}
