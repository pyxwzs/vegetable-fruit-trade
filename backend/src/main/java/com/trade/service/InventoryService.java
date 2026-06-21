package com.trade.service;

import com.trade.dto.InventoryMovementDTO;
import com.trade.dto.InventoryOverviewDTO;
import com.trade.entity.Inventory;
import com.trade.entity.Product;
import com.trade.entity.PurchaseOrder;
import com.trade.entity.Warehouse;
import com.trade.exception.BusinessException;
import com.trade.repository.InventoryRepository;
import com.trade.repository.ProductRepository;
import com.trade.repository.PurchaseOrderItemRepository;
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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final String DEFAULT_WAREHOUSE_CODE = "DEFAULT";
    private static final String DEFAULT_WAREHOUSE_NAME = "默认仓库";

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Transactional
    public Inventory addStock(InventoryMovementDTO dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new BusinessException("商品不存在"));
        Warehouse warehouse = resolveWarehouseForInbound(dto.getWarehouseId());

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
        return inventoryRepository.save(inv);
    }

    /** 入库：未指定仓库时使用已有仓库；若库中无任何仓库则自动创建默认仓库 */
    @Transactional
    public Warehouse resolveWarehouseForInbound(Long warehouseId) {
        if (warehouseId != null) {
            return warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new BusinessException("仓库不存在"));
        }
        Optional<Warehouse> existing = findAnyActiveWarehouse();
        if (existing.isPresent()) {
            return existing.get();
        }
        return createDefaultWarehouse();
    }

    /** 出库：须指定或存在可用仓库，不自动创建 */
    @Transactional(readOnly = true)
    public Warehouse requireWarehouse(Long warehouseId) {
        if (warehouseId != null) {
            return warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new BusinessException("仓库不存在"));
        }
        return findAnyActiveWarehouse()
                .orElseThrow(() -> new BusinessException("暂无仓库，请先完成入库"));
    }

    private Optional<Warehouse> findAnyActiveWarehouse() {
        return warehouseRepository.findByCode(DEFAULT_WAREHOUSE_CODE)
                .or(() -> warehouseRepository.findByStatusOrderByIdAsc(Warehouse.WarehouseStatus.ACTIVE)
                        .stream()
                        .findFirst());
    }

    private Warehouse createDefaultWarehouse() {
        return warehouseRepository.findByCode(DEFAULT_WAREHOUSE_CODE)
                .orElseGet(() -> {
                    Warehouse w = new Warehouse();
                    w.setCode(DEFAULT_WAREHOUSE_CODE);
                    w.setName(DEFAULT_WAREHOUSE_NAME);
                    w.setStatus(Warehouse.WarehouseStatus.ACTIVE);
                    return warehouseRepository.save(w);
                });
    }

    @Transactional
    public Inventory removeStock(InventoryMovementDTO dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new BusinessException("商品不存在"));
        Warehouse warehouse = requireWarehouse(dto.getWarehouseId());

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
        dto.setWarehouseId(requireWarehouse(warehouseId).getId());
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

    public InventoryOverviewDTO getOverview() {
        Map<Long, BigDecimal> latestPrice = loadLatestPurchasePriceByProduct();

        List<Inventory> inventories = inventoryRepository.findAll().stream()
                .filter(i -> i.getQuantity() != null && i.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        InventoryOverviewDTO overview = new InventoryOverviewDTO();
        overview.setSkuCount(inventories.size());

        BigDecimal totalValue = BigDecimal.ZERO;
        List<InventoryOverviewDTO.Item> items = new ArrayList<>();

        for (Inventory inv : inventories) {
            Product product = inv.getProduct();
            BigDecimal qty = inv.getQuantity();
            BigDecimal unitCost = latestPrice.getOrDefault(product.getId(), BigDecimal.ZERO);
            BigDecimal value = qty.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
            totalValue = totalValue.add(value);

            InventoryOverviewDTO.Item item = new InventoryOverviewDTO.Item();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setCategory(product.getCategory());
            item.setUnit(product.getUnit());
            item.setQuantity(qty);
            item.setUnitCost(unitCost);
            item.setValue(value);
            items.add(item);
        }

        overview.setTotalValue(totalValue);
        overview.setItems(items);
        return overview;
    }

    private Map<Long, BigDecimal> loadLatestPurchasePriceByProduct() {
        List<com.trade.entity.PurchaseOrderItem> rows = purchaseOrderItemRepository
                .findByPurchaseOrder_StatusNotOrderByPurchaseOrder_OrderDateDescIdDesc(
                        PurchaseOrder.OrderStatus.CANCELLED);

        Map<Long, BigDecimal> map = new HashMap<>();
        for (com.trade.entity.PurchaseOrderItem row : rows) {
            map.putIfAbsent(row.getProduct().getId(), row.getPrice());
        }
        return map;
    }
}
