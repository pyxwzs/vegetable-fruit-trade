package com.trade.repository;

import com.trade.entity.Inventory;
import com.trade.entity.Product;
import com.trade.entity.Warehouse;
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
public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {
    Optional<Inventory> findByProductAndWarehouseAndBatchNo(Product product, Warehouse warehouse, String batchNo);

    List<Inventory> findByProduct(Product product);

    List<Inventory> findByWarehouse(Warehouse warehouse);

    @Query("SELECT i FROM Inventory i WHERE i.expiryDate <= :date AND i.status != 'EXPIRED'")
    List<Inventory> findExpiringBefore(@Param("date") LocalDate date);

    /** 当前库存大于 0 且低于安全阈值（固定 10，与前端预警一致） */
    @Query("SELECT i FROM Inventory i WHERE i.quantity > 0 AND i.quantity < 10")
    List<Inventory> findLowStock();

    @Query("SELECT SUM(i.quantity) FROM Inventory i WHERE i.product.id = :productId")
    BigDecimal getTotalQuantityByProduct(@Param("productId") Long productId);

    /** 指定仓库内某商品有可用量的库存行（按 id 顺序出库，近似先进先出） */
    @Query("SELECT i FROM Inventory i WHERE i.product.id = :pid AND i.warehouse.id = :wid AND i.availableQuantity > 0 ORDER BY i.id ASC")
    List<Inventory> findAvailableStockFifo(@Param("pid") Long productId, @Param("wid") Long warehouseId);

    /** 全仓可用库存合计低于阈值的商品（与低库存预警逻辑一致，按商品汇总） */
    @Query("SELECT i.product.id, i.product.name, i.product.unit, COALESCE(SUM(i.availableQuantity), 0) FROM Inventory i GROUP BY i.product.id, i.product.name, i.product.unit HAVING COALESCE(SUM(i.availableQuantity), 0) < :threshold")
    List<Object[]> findProductsBelowAvailableThreshold(@Param("threshold") BigDecimal threshold);

    @Query("SELECT COUNT(DISTINCT i.product.id) FROM Inventory i WHERE i.quantity > 0")
    long countDistinctProductsInStock();

    /** 按仓库统计当前库存资产价值（数量 × 采购价） */
    @Query("SELECT i.warehouse.id, i.warehouse.name, COALESCE(SUM(i.quantity * i.purchasePrice), 0) FROM Inventory i WHERE i.quantity > 0 GROUP BY i.warehouse.id, i.warehouse.name ORDER BY i.warehouse.id")
    List<Object[]> sumAssetByWarehouse();

    /** 全部仓库库存资产总价值 */
    @Query("SELECT COALESCE(SUM(i.quantity * i.purchasePrice), 0) FROM Inventory i WHERE i.quantity > 0")
    Object sumTotalAsset();

    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity + :quantity WHERE i.id = :id")
    void addQuantity(@Param("id") Long id, @Param("quantity") BigDecimal quantity);

    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity - :quantity WHERE i.id = :id AND i.quantity >= :quantity")
    int subtractQuantity(@Param("id") Long id, @Param("quantity") BigDecimal quantity);
}