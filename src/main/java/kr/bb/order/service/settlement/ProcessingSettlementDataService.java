package kr.bb.order.service.settlement;

import bloomingblooms.domain.notification.delivery.DeliveryStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kr.bb.order.entity.pickup.OrderPickupStatus;
import kr.bb.order.entity.subscription.SubscriptionStatus;
import kr.bb.order.repository.OrderDeliveryRepository;
import kr.bb.order.repository.OrderPickupRepository;
import kr.bb.order.repository.OrderSubscriptionRepository;
import kr.bb.order.util.StoreIdAndTotalAmountProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ProcessingSettlementDataService {

  private final OrderDeliveryRepository orderDeliveryRepository;
  private final OrderPickupRepository orderPickupRepository;
  private final OrderSubscriptionRepository orderSubscriptionRepository;

  public List<StoreIdAndTotalAmountProjection> getTotalAmountByStoreId(LocalDateTime startDate, LocalDateTime endDate) {
    List<StoreIdAndTotalAmountProjection> deliveryAmount = orderDeliveryRepository.findAllStoreIdAndTotalAmountForDateRangeAndNotOrderPickupStatus(startDate,endDate,
        DeliveryStatus.CANCELED);
    List<StoreIdAndTotalAmountProjection> pickupAmount = orderPickupRepository.findAllStoreIdAndTotalAmountForDateRangeAndNotOrderPickupStatus(startDate,endDate,
        OrderPickupStatus.CANCELED);
    List<StoreIdAndTotalAmountProjection> subscriptionAmount = orderSubscriptionRepository.findAllStoreIdAndTotalAmountForDateRangeAndNotOrderPickupStatus(startDate,endDate,
        SubscriptionStatus.CANCELED);

    Map<Long, Long> totalAmountByStoreId = combineAndSumTotalAmount(deliveryAmount, pickupAmount,
        subscriptionAmount);

    return totalAmountByStoreId.entrySet()
        .stream()
        .map(entry -> new StoreIdAndTotalAmountProjection(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }

  private Map<Long, Long> combineAndSumTotalAmount(
      List<StoreIdAndTotalAmountProjection>... amounts) {
    Map<Long, Long> totalAmountByStoreId = new HashMap<>();

    for (List<StoreIdAndTotalAmountProjection> amountList : amounts) {
      for (StoreIdAndTotalAmountProjection amount : amountList) {
        totalAmountByStoreId.merge(amount.getStoreId(), amount.getTotalAmount(), Long::sum);
      }
    }

    return totalAmountByStoreId;
  }

}
