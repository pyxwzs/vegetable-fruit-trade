package com.trade.repository;

import com.trade.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {

    boolean existsBySupplier_Id(Long supplierId);

    @EntityGraph(attributePaths = {"supplier", "items", "items.product", "items.product.category"})
    Page<PurchaseOrder> findAll(Specification<PurchaseOrder> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"supplier", "items", "items.product", "items.product.category"})
    Optional<PurchaseOrder> findById(Long id);

    @Query("SELECT COUNT(p) FROM PurchaseOrder p WHERE p.status = :st")
    long countByStatus(@Param("st") PurchaseOrder.OrderStatus st);

    @Query("SELECT COUNT(p) FROM PurchaseOrder p WHERE p.orderDate = :d AND p.status <> 'CANCELLED'")
    long countOrdersOnDate(@Param("d") LocalDate d);

    @Query("SELECT COALESCE(SUM(p.totalAmount),0), COALESCE(SUM(p.paidAmount),0), COALESCE(SUM(p.totalAmount - COALESCE(p.paidAmount,0)),0) FROM PurchaseOrder p WHERE p.status <> 'CANCELLED'")
    List<Object[]> sumFinance();

    @Query("SELECT FUNCTION('DATE_FORMAT', p.orderDate, '%Y-%m'), COALESCE(SUM(p.totalAmount),0), COALESCE(SUM(p.paidAmount),0) FROM PurchaseOrder p WHERE p.status <> 'CANCELLED' AND p.orderDate >= :start GROUP BY FUNCTION('DATE_FORMAT', p.orderDate, '%Y-%m') ORDER BY 1")
    List<Object[]> sumFinanceByMonth(@Param("start") LocalDate start);

    Optional<PurchaseOrder> findByOrderNo(String orderNo);

    @Query("SELECT MONTH(p.orderDate), COUNT(p), COALESCE(SUM(p.totalAmount),0), COALESCE(SUM(p.paidAmount),0) " +
           "FROM PurchaseOrder p WHERE p.supplier.id = :supplierId AND YEAR(p.orderDate) = :year AND p.status <> 'CANCELLED' " +
           "GROUP BY MONTH(p.orderDate) ORDER BY MONTH(p.orderDate)")
    List<Object[]> monthlyStatsBySupplier(@Param("supplierId") Long supplierId, @Param("year") int year);

    @Query("SELECT MONTH(p.orderDate), COUNT(p), COALESCE(SUM(p.totalAmount),0), COALESCE(SUM(p.paidAmount),0) " +
           "FROM PurchaseOrder p WHERE YEAR(p.orderDate) = :year AND p.status <> 'CANCELLED' " +
           "GROUP BY MONTH(p.orderDate) ORDER BY MONTH(p.orderDate)")
    List<Object[]> monthlyStatsAllSuppliers(@Param("year") int year);

    @Query("SELECT p.orderDate, COUNT(p), COALESCE(SUM(p.totalAmount),0), COALESCE(SUM(p.paidAmount),0) " +
           "FROM PurchaseOrder p WHERE p.supplier.id = :supplierId " +
           "AND YEAR(p.orderDate) = :year AND MONTH(p.orderDate) = :month AND p.status <> 'CANCELLED' " +
           "GROUP BY p.orderDate ORDER BY p.orderDate")
    List<Object[]> dailyStatsBySupplier(@Param("supplierId") Long supplierId, @Param("year") int year, @Param("month") int month);

    @Query("SELECT p.orderDate, COUNT(p), COALESCE(SUM(p.totalAmount),0), COALESCE(SUM(p.paidAmount),0) " +
           "FROM PurchaseOrder p WHERE YEAR(p.orderDate) = :year AND MONTH(p.orderDate) = :month AND p.status <> 'CANCELLED' " +
           "GROUP BY p.orderDate ORDER BY p.orderDate")
    List<Object[]> dailyStatsAllSuppliers(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM PurchaseOrder o " +
           "WHERE YEAR(o.orderDate) = :year AND MONTH(o.orderDate) = :month AND o.status = 'COMPLETED'")
    BigDecimal sumCompletedByMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(o.totalAmount - COALESCE(o.paidAmount, 0)), 0) FROM PurchaseOrder o " +
           "WHERE YEAR(o.orderDate) = :year AND o.status = 'COMPLETED' AND o.paymentStatus <> 'PAID'")
    BigDecimal sumUnpaidByYear(@Param("year") int year);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM PurchaseOrder o WHERE o.status = 'PENDING'")
    java.math.BigDecimal sumPendingAmount();

    @Query("SELECT o.supplier.id, o.supplier.name, COUNT(o), COALESCE(SUM(o.totalAmount), 0), COALESCE(SUM(o.paidAmount), 0) " +
           "FROM PurchaseOrder o " +
           "WHERE YEAR(o.orderDate) = :year AND o.status <> 'CANCELLED' " +
           "GROUP BY o.supplier.id, o.supplier.name " +
           "ORDER BY (SUM(o.totalAmount) - SUM(o.paidAmount)) DESC")
    List<Object[]> supplierStatsByYear(@Param("year") int year);

    @Query("SELECT o.supplier.id, o.supplier.name, COUNT(o), COALESCE(SUM(o.totalAmount), 0), COALESCE(SUM(o.paidAmount), 0) " +
           "FROM PurchaseOrder o " +
           "WHERE YEAR(o.orderDate) = :year AND MONTH(o.orderDate) = :month AND o.status <> 'CANCELLED' " +
           "GROUP BY o.supplier.id, o.supplier.name " +
           "ORDER BY (SUM(o.totalAmount) - SUM(o.paidAmount)) DESC")
    List<Object[]> supplierStatsByMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT o.supplier.name, COALESCE(SUM(o.totalAmount), 0), COALESCE(SUM(o.paidAmount), 0) " +
           "FROM PurchaseOrder o " +
           "WHERE YEAR(o.orderDate) = :year AND MONTH(o.orderDate) = :month AND o.status = 'COMPLETED' " +
           "GROUP BY o.supplier.id, o.supplier.name " +
           "ORDER BY (SUM(o.totalAmount) - SUM(o.paidAmount)) DESC")
    List<Object[]> farmerBalanceRanking(@Param("year") int year, @Param("month") int month);

    @Query("SELECT i.product.name, i.product.unit, COALESCE(SUM(i.amount), 0) " +
           "FROM PurchaseOrderItem i JOIN i.purchaseOrder o " +
           "WHERE YEAR(o.orderDate) = :year AND MONTH(o.orderDate) = :month AND o.status = 'COMPLETED' " +
           "GROUP BY i.product.id, i.product.name, i.product.unit")
    List<Object[]> productPurchaseSumByMonth(@Param("year") int year, @Param("month") int month);
}
