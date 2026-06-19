package com.trade.controller;

import com.trade.dto.InventoryMovementDTO;
import com.trade.entity.Inventory;
import com.trade.service.InventoryService;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ApiResponse<Page<Inventory>> getInventories(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(inventoryService.getInventories(keyword, productId, warehouseId, pageable));
    }

    @PostMapping("/inbound")
    public ApiResponse<Inventory> inbound(@Valid @RequestBody InventoryMovementDTO dto) {
        return ApiResponse.success(inventoryService.addStock(dto));
    }

    @PostMapping("/outbound")
    public ApiResponse<Inventory> outbound(@Valid @RequestBody InventoryMovementDTO dto) {
        return ApiResponse.success(inventoryService.removeStock(dto));
    }

    @GetMapping("/low-stock")
    public ApiResponse<List<Inventory>> getLowStockProducts() {
        return ApiResponse.success(inventoryService.getLowStockProducts());
    }
}
