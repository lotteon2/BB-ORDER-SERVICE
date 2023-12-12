package kr.bb.order.repository;

import java.util.List;
import kr.bb.order.entity.OrderDeliveryProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderProductRepository extends JpaRepository<OrderDeliveryProduct, Long> {
  @Query("select op.productId from OrderDeliveryProduct op where op.orderId IN :orderIds")
  List<String> findProductIdsByOrderIds(@Param("orderIds") List<String> orderIds);

  @Query("select op from OrderDeliveryProduct op where op.orderId IN :orderIds")
  List<OrderDeliveryProduct> findAllByOrderIds(@Param("orderIds") List<String> orderIds);
}
