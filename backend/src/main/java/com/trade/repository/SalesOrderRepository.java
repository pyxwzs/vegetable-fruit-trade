package com.trade.repository;

import com.trade.entity.SalesOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long>, JpaSpecificationExecutor<SalesOrder> {

    boolean existsByCustomer_Id(Long customerId);

    @EntityGraph(attributePaths = {
            "customer",
            "salesman",
            "items", "items.product", "items.product.category"
    })
    Page<SalesOrder> findAll(Specification<SalesOrder> spec, Pageable pageable);

    @EntityGraph(attributePaths = {
            "customer",
            "salesman",
            "items", "items.product", "items.product.category"
    })
    Optional<SalesOrder> findById(Long id);

    @Query("SELECT COUNT(o) FROM SalesOrder o WHERE o.status = :st AND (:scope IS NULL OR o.salesman.id = :scope)")
    long countByStatusScoped(@Param("st") SalesOrder.OrderStatus st, @Param("scope") Long scopeUserId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM SalesOrder o WHERE o.orderDate >= :start AND o.orderDate <= :end AND o.status IN ('SHIPPED', 'DELIVERED', 'COMPLETED') AND (:scope IS NULL OR o.salesman.id = :scope)")
    BigDecimal sumRealizedSalesBetween(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("scope") Long scopeUserId);

    @Query("SELECT o.orderDate, COALESCE(SUM(o.totalAmount), 0) FROM SalesOrder o WHERE o.orderDate >= :start AND o.orderDate <= :end AND o.status IN ('SHIPPED', 'DELIVERED', 'COMPLETED') AND (:scope IS NULL OR o.salesman.id = :scope) GROUP BY o.orderDate ORDER BY o.orderDate")
    List<Object[]> sumRealizedSalesByDay(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("scope") Long scopeUserId);

    @Query("SELECT COUNT(o) FROM SalesOrder o WHERE o.orderDate >= :start AND o.orderDate <= :end AND o.status <> 'CANCELLED' AND (:scope IS NULL OR o.salesman.id = :scope)")
    long countOrdersBetween(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("scope") Long scopeUserId);

    @Query("SELECT COUNT(o) FROM SalesOrder o WHERE o.orderDate = :d AND o.status <> 'CANCELLED' AND (:scope IS NULL OR o.salesman.id = :scope)")
    long countOrdersOnDate(@Param("d") LocalDate d, @Param("scope") Long scopeUserId);

    @Query("SELECT COUNT(DISTINCT o.customer.id) FROM SalesOrder o WHERE o.orderDate >= :start AND o.orderDate <= :end AND o.status <> 'CANCELLED' AND (:scope IS NULL OR o.salesman.id = :scope)")
    long countDistinctCustomersWithOrders(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("scope") Long scopeUserId);

    Optional<SalesOrder> findByOrderNo(String orderNo);
    boolean existsByOrderNo(String orderNo);

    /** 销售汇总：总销售额、已收、未收（排除取消的订单） */
    @Query("SELECT COALESCE(SUM(o.totalAmount),0), COALESCE(SUM(o.receivedAmount),0), COALESCE(SUM(o.totalAmount - COALESCE(o.receivedAmount,0)),0) FROM SalesOrder o WHERE o.status <> 'CANCELLED'")
    List<Object[]> sumFinance();

    /** 按月汇总：月份, 销售额, 已收 */
    @Query("SELECT FUNCTION('DATE_FORMAT', o.orderDate, '%Y-%m'), COALESCE(SUM(o.totalAmount),0), COALESCE(SUM(o.receivedAmount),0) FROM SalesOrder o WHERE o.status <> 'CANCELLED' AND o.orderDate >= :start GROUP BY FUNCTION('DATE_FORMAT', o.orderDate, '%Y-%m') ORDER BY 1")
    List<Object[]> sumFinanceByMonth(@Param("start") LocalDate start);

    @Query("SELECT COALESCE(SUM(o.totalAmount - COALESCE(o.receivedAmount, 0)), 0) FROM SalesOrder o WHERE o.customer.id = :cid AND o.status <> 'CANCELLED' AND o.paymentStatus <> 'PAID'")
    BigDecimal sumUnpaidExposure(@Param("cid") Long customerId);

    @Modifying
    @Query("UPDATE SalesOrder o SET o.salesman = null WHERE o.salesman.id = :userId")
    void clearSalesmanByUserId(@Param("userId") Long userId);
}
