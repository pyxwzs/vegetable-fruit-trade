package com.trade.controller;

import com.trade.dto.*;
import com.trade.entity.SalesOrder;
import com.trade.service.ReturnRequestService;
import com.trade.service.SalesService;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;
    private final ReturnRequestService returnRequestService;

    @GetMapping
    public ApiResponse<Page<SalesOrder>> getSalesOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 5, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        SalesOrder.OrderStatus st = null;
        if (status != null && !status.isBlank()) {
            st = SalesOrder.OrderStatus.valueOf(status.trim().toUpperCase());
        }
        return ApiResponse.success(salesService.getSalesOrders(keyword, st, paymentStatus, startDate, endDate, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<SalesOrder> getSalesOrder(@PathVariable Long id) {
        return ApiResponse.success(salesService.getSalesOrder(id));
    }

    @PostMapping
    public ApiResponse<SalesOrder> createSalesOrder(@Valid @RequestBody SalesOrderDTO dto) {
        return ApiResponse.success(salesService.createSalesOrder(dto));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<SalesOrder> completeOrder(
            @PathVariable Long id,
            @RequestParam(required = false) Long warehouseId) {
        return ApiResponse.success(salesService.completeOrder(id, warehouseId));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<SalesOrder> cancelOrder(@PathVariable Long id) {
        return ApiResponse.success(salesService.cancelOrder(id));
    }

    @PostMapping("/{id}/return")
    public ApiResponse<ReturnRequestDTO> salesReturn(
            @PathVariable Long id, @Valid @RequestBody SalesReturnDTO dto) {
        return ApiResponse.success(returnRequestService.submitSalesReturn(id, dto));
    }

    @PostMapping("/{id}/collect")
    public ApiResponse<SalesOrder> recordReceipt(
            @PathVariable Long id, @RequestBody AddPaymentDTO dto) {
        return ApiResponse.success(salesService.recordReceipt(id, dto));
    }

    @GetMapping("/{id}/payments")
    public ApiResponse<List<PaymentRecordDTO>> getPayments(@PathVariable Long id) {
        return ApiResponse.success(salesService.getPaymentHistory(id));
    }

    @GetMapping("/pending-stats")
    public ApiResponse<java.util.Map<String, Object>> pendingStats() {
        return ApiResponse.success(salesService.getPendingStats());
    }
}
