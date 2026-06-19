package com.trade.repository;

import com.trade.entity.Inventory;
import com.trade.entity.Product;
import com.trade.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {

    boolean existsByProduct_Id(Long productId);

    Optional<Inventory> findByProductAndWarehouse(Product product, Warehouse warehouse);

    List<Inventory> findByProduct(Product product);

    List<Inventory> findByWarehouse(Warehouse warehouse);

    @Query("SELECT i FROM Inventory i WHERE i.quantity > 0 AND i.quantity < 10")
    List<Inventory> findLowStock();

    @Query("SELECT SUM(i.quantity) FROM Inventory i WHERE i.product.id = :productId")
    BigDecimal getTotalQuantityByProduct(@Param("productId") Long productId);

    @Query("SELECT COUNT(DISTINCT i.product.id) FROM Inventory i WHERE i.quantity > 0")
    long countDistinctProductsInStock();

    @Query("SELECT i.product.id, i.product.name, i.product.unit, COALESCE(SUM(i.quantity), 0) FROM Inventory i GROUP BY i.product.id, i.product.name, i.product.unit HAVING COALESCE(SUM(i.quantity), 0) < :threshold")
    List<Object[]> findLowStockByProduct(@Param("threshold") BigDecimal threshold);

    @Query("SELECT i.warehouse.id, i.warehouse.name, COALESCE(SUM(i.quantity), 0) FROM Inventory i WHERE i.quantity > 0 GROUP BY i.warehouse.id, i.warehouse.name ORDER BY i.warehouse.id")
    List<Object[]> sumAssetByWarehouse();

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i WHERE i.quantity > 0")
    Object sumTotalAsset();
}
