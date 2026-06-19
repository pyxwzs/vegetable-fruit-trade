package com.trade.repository;

import com.trade.entity.PurchaseOrder;
import com.trade.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    boolean existsByProduct_Id(Long productId);

    List<PurchaseOrderItem> findByPurchaseOrder_StatusNotOrderByPurchaseOrder_OrderDateDescIdDesc(
            PurchaseOrder.OrderStatus status);

    @Query("SELECT i.purchaseOrder.orderDate, o.supplier.name, o.orderNo, o.paymentStatus, " +
           "i.product.name, i.product.unit, i.product.specification, " +
           "i.quantity, i.price, i.amount " +
           "FROM PurchaseOrderItem i JOIN i.purchaseOrder o " +
           "WHERE o.supplier.id = :supplierId " +
           "AND YEAR(o.orderDate) = :year AND MONTH(o.orderDate) = :month AND o.status <> 'CANCELLED' " +
           "ORDER BY o.orderDate, i.product.name")
    List<Object[]> itemDetailBySupplier(@Param("supplierId") Long supplierId,
                                        @Param("year") int year, @Param("month") int month);

    @Query("SELECT i.purchaseOrder.orderDate, o.supplier.name, o.orderNo, o.paymentStatus, " +
           "i.product.name, i.product.unit, i.product.specification, " +
           "i.quantity, i.price, i.amount " +
           "FROM PurchaseOrderItem i JOIN i.purchaseOrder o " +
           "WHERE YEAR(o.orderDate) = :year AND MONTH(o.orderDate) = :month AND o.status <> 'CANCELLED' " +
           "ORDER BY o.orderDate, o.supplier.name, i.product.name")
    List<Object[]> itemDetailAll(@Param("year") int year, @Param("month") int month);
}
