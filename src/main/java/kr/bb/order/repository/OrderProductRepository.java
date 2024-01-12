package kr.bb.order.repository;

import java.util.List;
import kr.bb.order.entity.OrderDeliveryProduct;
import kr.bb.order.util.StoreIdAndTotalAmountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderProductRepository extends JpaRepository<OrderDeliveryProduct, Long> {

  @Query("select odp.productId from OrderDeliveryProduct odp where odp.orderDelivery.orderDeliveryId IN :orderIds")
  List<String> findProductIdsByOrderIds(@Param("orderIds") List<String> orderIds);

  @Query("select odp from OrderDeliveryProduct odp where odp.orderDelivery.orderDeliveryId IN :orderIds")
  List<OrderDeliveryProduct> findAllByOrderIds(@Param("orderIds") List<String> orderIds);


}
