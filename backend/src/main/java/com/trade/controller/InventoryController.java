package com.trade.controller;

import com.trade.dto.InventoryFreezeDTO;
import com.trade.dto.InventoryMovementDTO;
import com.trade.dto.StocktakeRequestDTO;
import com.trade.dto.TransferRequestDTO;
import com.trade.entity.Inventory;
import com.trade.service.InventoryService;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:view')")
    public ApiResponse<Page<Inventory>> getInventories(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Inventory> inventories = inventoryService.getInventories(keyword, productId, warehouseId, pageable);
        return ApiResponse.success(inventories);
    }

    @PostMapping("/inbound")
    @PreAuthorize("hasAuthority('inventory:inbound')")
    public ApiResponse<Inventory> inbound(@Valid @RequestBody InventoryMovementDTO movementDTO) {
        Inventory inventory = inventoryService.addStock(movementDTO);
        return ApiResponse.success(inventory);
    }

    @PostMapping("/outbound")
    @PreAuthorize("hasAuthority('inventory:outbound')")
    public ApiResponse<Inventory> outbound(@Valid @RequestBody InventoryMovementDTO movementDTO) {
        Inventory inventory = inventoryService.removeStock(movementDTO);
        return ApiResponse.success(inventory);
    }

    /** 盘库：仓管员调整实际库存数量 */
    @PostMapping("/stocktake")
    @PreAuthorize("hasAuthority('inventory:inbound')")
    public ApiResponse<Inventory> stocktake(@Valid @RequestBody StocktakeRequestDTO dto) {
        return ApiResponse.success(inventoryService.applyStocktake(dto));
    }

    /** 调拨：仓库间移库，同时需要出库和入库权限 */
    @PostMapping("/transfer")
    @PreAuthorize("hasAuthority('inventory:inbound') and hasAuthority('inventory:outbound')")
    public ApiResponse<Void> transfer(@Valid @RequestBody TransferRequestDTO dto) {
        inventoryService.transferBetweenWarehouses(dto);
        return ApiResponse.success(null);
    }

    @PostMapping("/freeze")
    @PreAuthorize("hasAuthority('inventory:inbound')")
    public ApiResponse<Inventory> freeze(@Valid @RequestBody InventoryFreezeDTO dto) {
        return ApiResponse.success(inventoryService.freezeQuantity(dto.getInventoryId(), dto.getQuantity()));
    }

    @PostMapping("/unfreeze")
    @PreAuthorize("hasAuthority('inventory:inbound')")
    public ApiResponse<Inventory> unfreeze(@Valid @RequestBody InventoryFreezeDTO dto) {
        return ApiResponse.success(inventoryService.unfreezeQuantity(dto.getInventoryId(), dto.getQuantity()));
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasAuthority('inventory:view')")
    public ApiResponse<List<Inventory>> getExpiringProducts() {
        List<Inventory> expiring = inventoryService.getExpiringProducts();
        return ApiResponse.success(expiring);
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAuthority('inventory:view')")
    public ApiResponse<List<Inventory>> getLowStockProducts() {
        List<Inventory> lowStock = inventoryService.getLowStockProducts();
        return ApiResponse.success(lowStock);
    }
}
