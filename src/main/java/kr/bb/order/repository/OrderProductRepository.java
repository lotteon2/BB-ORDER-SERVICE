package kr.bb.order.repository;

import java.util.List;
import kr.bb.order.entity.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderProductRepository extends JpaRepository<OrderProduct, Long> {
  @Query("select op.productId from OrderProduct op where op.orderId IN :orderIds")
  List<String> findProductIdsByOrderIds(@Param("orderIds") List<String> orderIds);

  @Query("select op from OrderProduct op where op.orderId IN :orderIds")
  List<OrderProduct> findAllByOrderIds(@Param("orderIds") List<String> orderIds);
}
