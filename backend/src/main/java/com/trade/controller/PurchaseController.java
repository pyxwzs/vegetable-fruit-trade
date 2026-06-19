package com.trade.controller;

import com.trade.dto.OrderPaymentDTO;
import com.trade.dto.PurchaseOrderDTO;
import com.trade.dto.PurchaseReturnDTO;
import com.trade.dto.ReturnRequestDTO;
import com.trade.entity.PurchaseOrder;
import com.trade.service.PurchaseService;
import com.trade.service.ReturnRequestService;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final ReturnRequestService returnRequestService;

    @GetMapping
    public ApiResponse<Page<PurchaseOrder>> getPurchaseOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PurchaseOrder.OrderStatus status,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(purchaseService.getPurchaseOrders(keyword, status, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<PurchaseOrder> getPurchaseOrder(@PathVariable Long id) {
        return ApiResponse.success(purchaseService.getPurchaseOrder(id));
    }

    @PostMapping
    public ApiResponse<PurchaseOrder> create(@Valid @RequestBody PurchaseOrderDTO dto) {
        return ApiResponse.success(purchaseService.createPurchaseOrder(dto));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<PurchaseOrder> complete(
            @PathVariable Long id,
            @RequestParam(required = false) Long warehouseId) {
        return ApiResponse.success(purchaseService.completeOrder(id, warehouseId));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<PurchaseOrder> cancel(@PathVariable Long id) {
        return ApiResponse.success(purchaseService.cancelOrder(id));
    }

    @PostMapping("/{id}/return")
    public ApiResponse<ReturnRequestDTO> purchaseReturn(
            @PathVariable Long id, @Valid @RequestBody PurchaseReturnDTO dto) {
        return ApiResponse.success(returnRequestService.submitPurchaseReturn(id, dto));
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<PurchaseOrder> recordPayment(
            @PathVariable Long id, @Valid @RequestBody OrderPaymentDTO dto) {
        return ApiResponse.success(purchaseService.recordPayment(id, dto.getAmount()));
    }
}
