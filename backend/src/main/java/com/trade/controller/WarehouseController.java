package com.trade.controller;

import com.trade.entity.Warehouse;
import com.trade.repository.WarehouseRepository;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/warehouses")
@RequiredArgsConstructor
public class WarehouseController {
    private final WarehouseRepository warehouseRepository;

    @GetMapping("/active")
    public ApiResponse<List<Warehouse>> listActive() {
        List<Warehouse> list = warehouseRepository.findByStatusOrderByIdAsc(Warehouse.WarehouseStatus.ACTIVE);
        return ApiResponse.success(list);
    }
}
