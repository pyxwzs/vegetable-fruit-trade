package com.trade.repository;

import com.trade.entity.SalesOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, Long> {

    boolean existsByProduct_Id(Long productId);

    @Query("SELECT i.product.id, i.product.name, COALESCE(SUM(i.amount), 0) FROM SalesOrderItem i JOIN i.salesOrder o WHERE o.orderDate >= :start AND o.orderDate <= :end AND o.status IN ('SHIPPED', 'DELIVERED', 'COMPLETED') AND (:scope IS NULL OR o.salesman.id = :scope) GROUP BY i.product.id, i.product.name ORDER BY SUM(i.amount) DESC")
    List<Object[]> sumSalesByProduct(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("scope") Long scopeUserId);

    @Query("SELECT COALESCE(SUM(i.amount), 0), COALESCE(SUM(i.quantity * COALESCE(p.purchasePrice, 0)), 0) FROM SalesOrderItem i JOIN i.salesOrder o JOIN i.product p WHERE o.orderDate >= :start AND o.orderDate <= :end AND o.status IN ('SHIPPED', 'DELIVERED', 'COMPLETED') AND (:scope IS NULL OR o.salesman.id = :scope)")
    Object[] sumRevenueAndEstimatedCost(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("scope") Long scopeUserId);

    @Query(value = "SELECT COALESCE(c.name, '未分类'), COALESCE(SUM(i.amount), 0) "
            + "FROM sales_order_items i "
            + "INNER JOIN sales_orders o ON i.order_id = o.id "
            + "INNER JOIN products p ON i.product_id = p.id "
            + "LEFT JOIN categories c ON p.category_id = c.id "
            + "WHERE o.order_date >= :start AND o.order_date <= :end "
            + "AND o.status IN ('SHIPPED', 'DELIVERED', 'COMPLETED') "
            + "AND (:scope IS NULL OR o.salesman_id = :scope) "
            + "GROUP BY COALESCE(c.id, -1), COALESCE(c.name, '未分类')",
            nativeQuery = true)
    List<Object[]> sumSalesByCategory(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("scope") Long scopeUserId);

    @Query("SELECT o.customer.id, o.customer.name, o.customer.creditLevel, COUNT(DISTINCT o.id), COALESCE(SUM(i.amount), 0), COALESCE(SUM(i.quantity * COALESCE(p.purchasePrice, 0)), 0) FROM SalesOrderItem i JOIN i.salesOrder o JOIN i.product p WHERE o.orderDate >= :start AND o.orderDate <= :end AND o.status IN ('SHIPPED', 'DELIVERED', 'COMPLETED') AND (:scope IS NULL OR o.salesman.id = :scope) GROUP BY o.customer.id, o.customer.name, o.customer.creditLevel ORDER BY SUM(i.amount) DESC")
    List<Object[]> customerValueStats(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("scope") Long scopeUserId);
}
