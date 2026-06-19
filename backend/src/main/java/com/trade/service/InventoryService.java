package com.trade.service;

import com.trade.dto.InventoryMovementDTO;
import com.trade.entity.Inventory;
import com.trade.entity.Product;
import com.trade.entity.Warehouse;
import com.trade.exception.BusinessException;
import com.trade.repository.InventoryRepository;
import com.trade.repository.ProductRepository;
import com.trade.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    @Transactional
    public Inventory addStock(InventoryMovementDTO dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new BusinessException("商品不存在"));
        Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                .orElseThrow(() -> new BusinessException("仓库不存在"));

        Inventory inv = inventoryRepository.findByProductAndWarehouse(product, warehouse)
                .orElse(null);

        if (inv == null) {
            inv = new Inventory();
            inv.setProduct(product);
            inv.setWarehouse(warehouse);
            inv.setQuantity(dto.getQuantity());
        } else {
            inv.setQuantity(inv.getQuantity().add(dto.getQuantity()));
        }
        if (dto.getRemark() != null && !dto.getRemark().isBlank()) {
            inv.setRemark(dto.getRemark());
        }
        return inventoryRepository.save(inv);
    }

    @Transactional
    public Inventory removeStock(InventoryMovementDTO dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new BusinessException("商品不存在"));
        Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                .orElseThrow(() -> new BusinessException("仓库不存在"));

        Inventory inv = inventoryRepository.findByProductAndWarehouse(product, warehouse)
                .orElseThrow(() -> new BusinessException("库存记录不存在"));

        if (inv.getQuantity().compareTo(dto.getQuantity()) < 0) {
            throw new BusinessException("库存不足");
        }
        inv.setQuantity(inv.getQuantity().subtract(dto.getQuantity()));
        return inventoryRepository.save(inv);
    }

    @Transactional
    public void deductStock(Long productId, Long warehouseId, BigDecimal quantity) {
        InventoryMovementDTO dto = new InventoryMovementDTO();
        dto.setProductId(productId);
        dto.setWarehouseId(warehouseId);
        dto.setQuantity(quantity);
        removeStock(dto);
    }

    public List<Inventory> getLowStockProducts() {
        return inventoryRepository.findLowStock();
    }

    public Page<Inventory> getInventories(String keyword, Long productId, Long warehouseId, Pageable pageable) {
        Specification<Inventory> spec = (root, query, cb) -> {
            if (query != null) query.distinct(true);
            List<Predicate> ps = new ArrayList<>();
            if (productId != null) {
                ps.add(cb.equal(root.get("product").get("id"), productId));
            }
            if (warehouseId != null) {
                ps.add(cb.equal(root.get("warehouse").get("id"), warehouseId));
            }
            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword.trim() + "%";
                Join<Object, Object> productJoin = root.join("product");
                ps.add(cb.or(
                        cb.like(productJoin.get("name"), kw),
                        cb.like(productJoin.get("productCode"), kw)
                ));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return inventoryRepository.findAll(spec, pageable);
    }
}
