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
}
