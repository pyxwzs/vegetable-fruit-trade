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

    @Query("SELECT i.product.id, i.product.name, COALESCE(SUM(i.amount), 0) FROM SalesOrderItem i JOIN i.salesOrder o WHERE o.orderDate >= :start AND o.orderDate <= :end AND o.status = 'COMPLETED' GROUP BY i.product.id, i.product.name ORDER BY SUM(i.amount) DESC")
    List<Object[]> sumSalesByProduct(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(i.amount), 0), 0 FROM SalesOrderItem i JOIN i.salesOrder o WHERE o.orderDate >= :start AND o.orderDate <= :end AND o.status = 'COMPLETED'")
    Object[] sumRevenueAndEstimatedCost(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT o.customer.id, o.customer.name, COUNT(DISTINCT o.id), COALESCE(SUM(i.amount), 0), 0 FROM SalesOrderItem i JOIN i.salesOrder o WHERE o.orderDate >= :start AND o.orderDate <= :end AND o.status = 'COMPLETED' GROUP BY o.customer.id, o.customer.name ORDER BY SUM(i.amount) DESC")
    List<Object[]> customerValueStats(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT i.salesOrder.orderDate, o.customer.name, o.orderNo, o.paymentStatus, " +
           "i.product.name, i.product.unit, i.product.specification, " +
           "i.quantity, i.price, i.amount " +
           "FROM SalesOrderItem i JOIN i.salesOrder o " +
           "WHERE o.customer.id = :customerId " +
           "AND YEAR(o.orderDate) = :year AND MONTH(o.orderDate) = :month AND o.status <> 'CANCELLED' " +
           "ORDER BY o.orderDate, i.product.name")
    List<Object[]> itemDetailByCustomer(@Param("customerId") Long customerId,
                                        @Param("year") int year, @Param("month") int month);

    @Query("SELECT i.salesOrder.orderDate, o.customer.name, o.orderNo, o.paymentStatus, " +
           "i.product.name, i.product.unit, i.product.specification, " +
           "i.quantity, i.price, i.amount " +
           "FROM SalesOrderItem i JOIN i.salesOrder o " +
           "WHERE YEAR(o.orderDate) = :year AND MONTH(o.orderDate) = :month AND o.status <> 'CANCELLED' " +
           "ORDER BY o.orderDate, o.customer.name, i.product.name")
    List<Object[]> itemDetailAll(@Param("year") int year, @Param("month") int month);

    @Query("SELECT o.customer.id, o.customer.name, i.product.id, i.product.name, i.product.unit, " +
           "COALESCE(SUM(i.quantity), 0), COALESCE(SUM(i.amount), 0) " +
           "FROM SalesOrderItem i JOIN i.salesOrder o " +
           "WHERE o.status <> 'CANCELLED' AND YEAR(o.orderDate) = :year " +
           "AND (:month = 0 OR MONTH(o.orderDate) = :month) " +
           "AND (:customerId IS NULL OR o.customer.id = :customerId) " +
           "GROUP BY o.customer.id, o.customer.name, i.product.id, i.product.name, i.product.unit " +
           "ORDER BY o.customer.name, SUM(i.amount) DESC")
    List<Object[]> customerProductStats(@Param("year") int year,
                                        @Param("month") int month,
                                        @Param("customerId") Long customerId);
}
