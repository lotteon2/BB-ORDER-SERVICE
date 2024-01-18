package kr.bb.order.service;

import bloomingblooms.domain.StatusChangeDto;
import bloomingblooms.domain.card.CardStatus;
import bloomingblooms.domain.review.ReviewStatus;
import javax.persistence.EntityNotFoundException;
import kr.bb.order.dto.ProductStatusChangeDto;
import kr.bb.order.entity.OrderDeliveryProduct;
import kr.bb.order.entity.OrderPickupProduct;
import kr.bb.order.repository.OrderDeliveryProductRepository;
import kr.bb.order.repository.OrderPickupProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderSqsService {
  private final OrderDeliveryProductRepository orderDeliveryProductRepository;
  private final OrderPickupProductRepository orderPickupProductRepository;

  @Transactional
  public void updateOrderDeliveryReview(ProductStatusChangeDto statusChangeDto) {
    OrderDeliveryProduct orderDeliveryProduct =
        orderDeliveryProductRepository
            .findById(statusChangeDto.getId())
            .orElseThrow(EntityNotFoundException::new);
    orderDeliveryProduct.updateReviewStatus(ReviewStatus.DONE);
  }

  @Transactional
  public void updateOrderDeliveryCard(ProductStatusChangeDto statusChangeDto) {
    OrderDeliveryProduct orderDeliveryProduct =
        orderDeliveryProductRepository
            .findById(statusChangeDto.getId())
            .orElseThrow(EntityNotFoundException::new);
    orderDeliveryProduct.updateCardStatus(CardStatus.DONE);
  }

  @Transactional
  public void updateOrderPickupCard(StatusChangeDto statusChangeDto) {
    // 카드에서 orderProductId를 가지므로 ~product entity에서 조회하는게 맞음
    OrderPickupProduct orderPickupProduct =
        orderPickupProductRepository
            .findById(Long.valueOf(statusChangeDto.getId()))
            .orElseThrow(EntityNotFoundException::new);
    orderPickupProduct.updateCardStatus(CardStatus.DONE);
  }

  @Transactional
  public void updateOrderPickupReview(StatusChangeDto statusChangeDto) {
    OrderPickupProduct orderPickupProduct =
        orderPickupProductRepository
            .findById(Long.valueOf(statusChangeDto.getId()))
            .orElseThrow(EntityNotFoundException::new);
    orderPickupProduct.updateReviewStatus(ReviewStatus.DONE);
  }
}
