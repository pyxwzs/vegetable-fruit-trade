package com.trade.service;

import com.trade.dto.InventoryMovementDTO;
import com.trade.dto.StocktakeRequestDTO;
import com.trade.dto.TransferRequestDTO;
import com.trade.entity.Inventory;
import com.trade.entity.InventoryStocktakeLog;
import com.trade.entity.InventoryTransferLog;
import com.trade.entity.Product;
import com.trade.entity.Warehouse;
import com.trade.exception.BusinessException;
import com.trade.repository.InventoryRepository;
import com.trade.repository.InventoryStocktakeLogRepository;
import com.trade.repository.InventoryTransferLogRepository;
import com.trade.repository.ProductRepository;
import com.trade.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryStocktakeLogRepository stocktakeLogRepository;
    private final InventoryTransferLogRepository transferLogRepository;
    private final AlertService alertService;

    private static String normalizeBatch(String batchNo) {
        if (batchNo == null) {
            return null;
        }
        String t = batchNo.trim();
        return t.isEmpty() ? null : t;
    }

    private static String currentOperator() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public Inventory addStock(InventoryMovementDTO movementDTO) {
        Product product = productRepository.findById(movementDTO.getProductId())
                .orElseThrow(() -> new BusinessException("商品不存在"));

        Warehouse warehouse = warehouseRepository.findById(movementDTO.getWarehouseId())
                .orElseThrow(() -> new BusinessException("仓库不存在"));

        String batchNo = normalizeBatch(movementDTO.getBatchNo());

        Inventory inventory = inventoryRepository
                .findByProductAndWarehouseAndBatchNo(product, warehouse, batchNo)
                .orElse(null);

        if (inventory == null) {
            inventory = new Inventory();
            inventory.setProduct(product);
            inventory.setWarehouse(warehouse);
            inventory.setBatchNo(batchNo);
            inventory.setProductionDate(movementDTO.getProductionDate());

            if (movementDTO.getExpiryDate() != null) {
                inventory.setExpiryDate(movementDTO.getExpiryDate());
            } else if (movementDTO.getProductionDate() != null && product.getShelfLife() != null) {
                inventory.setExpiryDate(movementDTO.getProductionDate().plusDays(product.getShelfLife()));
            }

            inventory.setPurchasePrice(movementDTO.getPrice());
            inventory.setLocation(movementDTO.getLocation());
            inventory.setQuantity(movementDTO.getQuantity());
            inventory.setAvailableQuantity(movementDTO.getQuantity());
        } else {
            inventory.setQuantity(inventory.getQuantity().add(movementDTO.getQuantity()));
            inventory.setAvailableQuantity(inventory.getAvailableQuantity().add(movementDTO.getQuantity()));
        }

        return inventoryRepository.save(inventory);
    }

    @Transactional
    public Inventory removeStock(InventoryMovementDTO movementDTO) {
        Product product = productRepository.findById(movementDTO.getProductId())
                .orElseThrow(() -> new BusinessException("商品不存在"));

        Warehouse warehouse = warehouseRepository.findById(movementDTO.getWarehouseId())
                .orElseThrow(() -> new BusinessException("仓库不存在"));

        String batchNo = normalizeBatch(movementDTO.getBatchNo());

        Inventory inventory = inventoryRepository
                .findByProductAndWarehouseAndBatchNo(product, warehouse, batchNo)
                .orElseThrow(() -> new BusinessException("库存记录不存在"));

        if (inventory.getAvailableQuantity().compareTo(movementDTO.getQuantity()) < 0) {
            throw new BusinessException("库存不足");
        }

        inventory.setAvailableQuantity(inventory.getAvailableQuantity().subtract(movementDTO.getQuantity()));
        inventory.setQuantity(inventory.getQuantity().subtract(movementDTO.getQuantity()));

        return inventoryRepository.save(inventory);
    }

    public List<Inventory> getExpiringProducts() {
        LocalDate warningDate = LocalDate.now().plusDays(7);
        return inventoryRepository.findExpiringBefore(warningDate);
    }

    public List<Inventory> getLowStockProducts() {
        return inventoryRepository.findLowStock();
    }

    public Page<Inventory> getInventories(String keyword, Long productId, Long warehouseId, Pageable pageable) {
        Specification<Inventory> spec = (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
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
                        cb.like(productJoin.get("productCode"), kw),
                        cb.like(cb.coalesce(root.get("batchNo"), cb.literal("")), kw)
                ));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return inventoryRepository.findAll(spec, pageable);
    }

    /**
     * 盘点：将指定库存行账面数量调整为实盘数量，产生盘盈/盘亏记录。
     */
    @Transactional
    public Inventory applyStocktake(StocktakeRequestDTO dto) {
        Inventory inv = inventoryRepository.findById(dto.getInventoryId())
                .orElseThrow(() -> new BusinessException("库存记录不存在"));
        BigDecimal actual = dto.getActualQuantity();
        BigDecimal frozen = inv.getFrozenQuantity() != null ? inv.getFrozenQuantity() : BigDecimal.ZERO;
        if (actual.compareTo(frozen) < 0) {
            throw new BusinessException("实盘数量不能小于当前冻结数量");
        }
        BigDecimal before = inv.getQuantity();
        BigDecimal diff = actual.subtract(before);
        inv.setQuantity(actual);
        inv.setAvailableQuantity(actual.subtract(frozen));
        Inventory saved = inventoryRepository.save(inv);

        InventoryStocktakeLog log = new InventoryStocktakeLog();
        log.setInventory(saved);
        log.setQtyBefore(before);
        log.setQtyAfter(actual);
        log.setDiffQty(diff);
        log.setRemark(dto.getRemark());
        log.setOperatorUsername(currentOperator());
        stocktakeLogRepository.save(log);

        return saved;
    }

    /**
     * 仓库间调拨：从指定库存行扣减可用数量，在目标仓库增加同批次库存。
     */
    @Transactional
    public void transferBetweenWarehouses(TransferRequestDTO dto) {
        Inventory src = inventoryRepository.findById(dto.getSourceInventoryId())
                .orElseThrow(() -> new BusinessException("源库存记录不存在"));
        Warehouse toWh = warehouseRepository.findById(dto.getToWarehouseId())
                .orElseThrow(() -> new BusinessException("目标仓库不存在"));
        if (src.getWarehouse().getId().equals(toWh.getId())) {
            throw new BusinessException("目标仓库不能与源仓库相同");
        }
        BigDecimal q = dto.getQuantity();
        if (src.getAvailableQuantity().compareTo(q) < 0) {
            throw new BusinessException("可用库存不足，无法调拨");
        }

        src.setQuantity(src.getQuantity().subtract(q));
        src.setAvailableQuantity(src.getAvailableQuantity().subtract(q));
        inventoryRepository.save(src);

        InventoryMovementDTO in = new InventoryMovementDTO();
        in.setProductId(src.getProduct().getId());
        in.setWarehouseId(toWh.getId());
        in.setBatchNo(src.getBatchNo());
        in.setQuantity(q);
        in.setPrice(src.getPurchasePrice());
        in.setProductionDate(src.getProductionDate());
        in.setExpiryDate(src.getExpiryDate());
        in.setLocation(src.getLocation());
        in.setRemark(dto.getRemark());
        addStock(in);

        InventoryTransferLog log = new InventoryTransferLog();
        log.setSourceInventoryId(src.getId());
        log.setFromWarehouse(src.getWarehouse());
        log.setToWarehouse(toWh);
        log.setProduct(src.getProduct());
        log.setBatchNo(src.getBatchNo());
        log.setQuantity(q);
        log.setRemark(dto.getRemark());
        log.setOperatorUsername(currentOperator());
        transferLogRepository.save(log);
    }

    /**
     * 冻结库存（可用转冻结），用于订单占用等场景。
     */
    @Transactional
    public Inventory freezeQuantity(Long inventoryId, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("冻结数量必须大于 0");
        }
        Inventory inv = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new BusinessException("库存记录不存在"));
        BigDecimal avail = inv.getAvailableQuantity() != null ? inv.getAvailableQuantity() : BigDecimal.ZERO;
        if (avail.compareTo(quantity) < 0) {
            throw new BusinessException("可用库存不足");
        }
        BigDecimal frozen = inv.getFrozenQuantity() != null ? inv.getFrozenQuantity() : BigDecimal.ZERO;
        inv.setAvailableQuantity(avail.subtract(quantity));
        inv.setFrozenQuantity(frozen.add(quantity));
        return inventoryRepository.save(inv);
    }

    /**
     * 解冻库存（冻结转可用）。
     */
    @Transactional
    public Inventory unfreezeQuantity(Long inventoryId, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("解冻数量必须大于 0");
        }
        Inventory inv = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new BusinessException("库存记录不存在"));
        BigDecimal frozen = inv.getFrozenQuantity() != null ? inv.getFrozenQuantity() : BigDecimal.ZERO;
        if (frozen.compareTo(quantity) < 0) {
            throw new BusinessException("冻结数量不足");
        }
        inv.setFrozenQuantity(frozen.subtract(quantity));
        BigDecimal avail = inv.getAvailableQuantity() != null ? inv.getAvailableQuantity() : BigDecimal.ZERO;
        inv.setAvailableQuantity(avail.add(quantity));
        return inventoryRepository.save(inv);
    }

    /**
     * 从指定仓库按库存行顺序扣减可用数量（销售发货等）。
     */
    @Transactional
    public void deductStockFifo(Long productId, Long warehouseId, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("扣减数量必须大于 0");
        }
        List<Inventory> lines = inventoryRepository.findAvailableStockFifo(productId, warehouseId);
        BigDecimal remain = quantity;
        for (Inventory inv : lines) {
            if (remain.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal avail = inv.getAvailableQuantity() != null ? inv.getAvailableQuantity() : BigDecimal.ZERO;
            BigDecimal take = avail.min(remain);
            if (take.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            InventoryMovementDTO m = new InventoryMovementDTO();
            m.setProductId(productId);
            m.setWarehouseId(warehouseId);
            m.setBatchNo(inv.getBatchNo());
            m.setQuantity(take);
            removeStock(m);
            remain = remain.subtract(take);
        }
        if (remain.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("库存不足（商品ID: " + productId + "）");
        }
    }

    @Transactional
    public void checkAndAlertExpiringProducts() {
        List<Inventory> expiringProducts = getExpiringProducts();
        for (Inventory inventory : expiringProducts) {
            alertService.sendExpiringAlert(inventory);
        }
    }

    @Transactional
    public void checkAndAlertLowStock() {
        List<Inventory> lowStockProducts = getLowStockProducts();
        for (Inventory inventory : lowStockProducts) {
            alertService.sendLowStockAlert(inventory);
        }
    }
}
