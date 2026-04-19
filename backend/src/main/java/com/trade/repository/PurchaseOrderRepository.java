package com.trade.repository;

import com.trade.entity.PurchaseOrder;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {

    /**
     * 分页列表与详情序列化 JSON 时需一并取出关联。
     * 注意：不能包含 purchaser.roles.permissions，否则多集合 JOIN 会产生笛卡尔积，
     * 导致 items 被重复（权限数量 × items 数量）。roles 已在实体层用 @JsonIgnoreProperties 屏蔽。
     */
    @EntityGraph(attributePaths = {
            "supplier",
            "purchaser",
            "items", "items.product", "items.product.category"
    })
    Page<PurchaseOrder> findAll(Specification<PurchaseOrder> spec, Pageable pageable);

    @EntityGraph(attributePaths = {
            "supplier",
            "purchaser",
            "items", "items.product", "items.product.category"
    })
    Optional<PurchaseOrder> findById(Long id);

    @Query("SELECT COUNT(p) FROM PurchaseOrder p WHERE p.status = :st AND (:scope IS NULL OR p.purchaser.id = :scope)")
    long countByStatusScoped(@Param("st") PurchaseOrder.OrderStatus st, @Param("scope") Long scopeUserId);

    @Query("SELECT COUNT(p) FROM PurchaseOrder p WHERE p.orderDate = :d AND p.status <> 'CANCELLED' AND (:scope IS NULL OR p.purchaser.id = :scope)")
    long countOrdersOnDate(@Param("d") LocalDate d, @Param("scope") Long scopeUserId);

    /** 采购汇总：总采购额、已付、未付（排除取消的订单） */
    @Query("SELECT COALESCE(SUM(p.totalAmount),0), COALESCE(SUM(p.paidAmount),0), COALESCE(SUM(p.totalAmount - COALESCE(p.paidAmount,0)),0) FROM PurchaseOrder p WHERE p.status <> 'CANCELLED'")
    List<Object[]> sumFinance();

    /** 按月汇总：月份, 采购额, 已付 */
    @Query("SELECT FUNCTION('DATE_FORMAT', p.orderDate, '%Y-%m'), COALESCE(SUM(p.totalAmount),0), COALESCE(SUM(p.paidAmount),0) FROM PurchaseOrder p WHERE p.status <> 'CANCELLED' AND p.orderDate >= :start GROUP BY FUNCTION('DATE_FORMAT', p.orderDate, '%Y-%m') ORDER BY 1")
    List<Object[]> sumFinanceByMonth(@Param("start") LocalDate start);

    Optional<PurchaseOrder> findByOrderNo(String orderNo);
    boolean existsByOrderNo(String orderNo);

    @Modifying
    @Query("UPDATE PurchaseOrder p SET p.purchaser = null WHERE p.purchaser.id = :userId")
    void clearPurchaserByUserId(@Param("userId") Long userId);
}
