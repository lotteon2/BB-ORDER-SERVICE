package kr.bb.order.service.settlement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kr.bb.order.dto.request.settlement.SettlementStoreInfoResponse;
import kr.bb.order.dto.request.settlement.StoreSettlementDto;
import kr.bb.order.dto.request.settlement.UpdateSettlementCommand;
import kr.bb.order.entity.settlement.Settlement;
import kr.bb.order.feign.StoreServiceClient;
import kr.bb.order.infra.OrderSQSPublisher;
import kr.bb.order.kafka.KafkaProducer;
import kr.bb.order.repository.settlement.SettlementJpaRepository;
import kr.bb.order.util.StoreIdAndTotalAmountProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.aws.messaging.listener.Acknowledgment;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class SettlementDataHandler {

  private final SettlementJpaRepository settlementJpaRepository;
  private final ProcessingSettlementDataService processingSettlementDataService;
  private final StoreServiceClient storeServiceClient;
  private final OrderSQSPublisher orderSQSPublisher;
  private final KafkaProducer<UpdateSettlementCommand> kafkaProducer;


  @Transactional
  @SqsListener(
      value = "${cloud.aws.sqs.settlement-trigger-queue.name}",
      deletionPolicy = SqsMessageDeletionPolicy.NEVER)
  public void handleTheSettlementEvent(
      @Payload String message, @Headers Map<String, String> headers, Acknowledgment ack) {

    List<StoreIdAndTotalAmountProjection> storeIdAndTotalAmountProjections = saveSettlement();
    orderSQSPublisher.publishStoreSettlement(
        getStoreIdFromProjection(storeIdAndTotalAmountProjections));
    kafkaProducer.send("settlement",
        convertToUpdateSettlementCommand(storeIdAndTotalAmountProjections));

    ack.acknowledge();

  }

  private UpdateSettlementCommand convertToUpdateSettlementCommand(
      List<StoreIdAndTotalAmountProjection> storeIdAndTotalAmountProjections) {

    List<StoreSettlementDto> storeSettlementDtoList = storeIdAndTotalAmountProjections.stream()
        .map(projection -> StoreSettlementDto.builder()
            .storeId(projection.getStoreId())
            .settlementAmount(projection.getTotalAmount())
            .build())
        .collect(Collectors.toList());

    return new UpdateSettlementCommand(storeSettlementDtoList);
  }

  /**
   * @return return the List<Long> for publish the SQS publish
   */

  public List<StoreIdAndTotalAmountProjection> saveSettlement() {

    LocalDateTime endDate = LocalDateTime.now().minusDays(1);
    LocalDateTime startDate = endDate.minusDays(30);

    List<StoreIdAndTotalAmountProjection> totalAmountByStoreId = processingSettlementDataService.getTotalAmountByStoreId(
        startDate, endDate);

    List<Long> storeIdList = getStoreIdFromProjection(totalAmountByStoreId);
    List<SettlementStoreInfoResponse> settlementStoreInfoResponseList = storeServiceClient.getStoreInfoByStoreId(
        storeIdList).getData();

    settlementJpaRepository.saveAll(
        combineAndCreateSettlements(settlementStoreInfoResponseList, totalAmountByStoreId));
    return totalAmountByStoreId;

  }

  private List<Long> getStoreIdFromProjection(
      List<StoreIdAndTotalAmountProjection> projectionList) {
    List<Long> storeIdList = new ArrayList<>();
    for (StoreIdAndTotalAmountProjection p : projectionList) {
      storeIdList.add(p.getStoreId());
    }
    return storeIdList;
  }

  public List<Settlement> combineAndCreateSettlements(
      List<SettlementStoreInfoResponse> storeInfoResponses,
      List<StoreIdAndTotalAmountProjection> totalAmountProjections) {

    Map<Long, StoreIdAndTotalAmountProjection> totalAmountMap = totalAmountProjections.stream()
        .collect(Collectors.toMap(StoreIdAndTotalAmountProjection::getStoreId,
            projection -> projection));

    return storeInfoResponses.stream()
        .map(storeInfo -> createSettlementFromInfoAndTotalAmount(storeInfo,
            totalAmountMap.get(storeInfo.getStoreId())))
        .collect(Collectors.toList());
  }

  private Settlement createSettlementFromInfoAndTotalAmount(SettlementStoreInfoResponse storeInfo,
      StoreIdAndTotalAmountProjection totalAmountProjection) {
    return Settlement.builder()
        .storeName(storeInfo.getStoreName())
        .settlementDate(LocalDateTime.now())
        .settlementAmount(
            totalAmountProjection != null ? totalAmountProjection.getTotalAmount() : 0L)
        .bankName(storeInfo.getBankName())
        .accountNumber(storeInfo.getAccountNumber())
        .storeId(storeInfo.getStoreId())
        .totalAmountPerMonth(
            totalAmountProjection != null ? (long) (totalAmountProjection.getTotalAmount() * 0.93)
                : 0L)
        .gugun(storeInfo.getGugun())
        .sido(storeInfo.getSido())
        .build();
  }

}
