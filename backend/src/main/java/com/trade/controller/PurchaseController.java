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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/purchase")
@RequiredArgsConstructor
public class PurchaseController {
    private final PurchaseService purchaseService;
    private final ReturnRequestService returnRequestService;

    @GetMapping
    @PreAuthorize("hasAuthority('purchase:view')")
    public ApiResponse<Page<PurchaseOrder>> getPurchaseOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PurchaseOrder.OrderStatus status,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(purchaseService.getPurchaseOrders(keyword, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('purchase:view')")
    public ApiResponse<PurchaseOrder> getPurchaseOrder(@PathVariable Long id) {
        return ApiResponse.success(purchaseService.getPurchaseOrder(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('purchase:create')")
    public ApiResponse<PurchaseOrder> create(@Valid @RequestBody PurchaseOrderDTO dto) {
        return ApiResponse.success(purchaseService.createPurchaseOrder(dto));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('purchase:approve')")
    public ApiResponse<PurchaseOrder> approve(@PathVariable Long id) {
        return ApiResponse.success(purchaseService.approveOrder(id));
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize("hasAuthority('purchase:approve')")
    public ApiResponse<PurchaseOrder> ship(@PathVariable Long id) {
        return ApiResponse.success(purchaseService.shipOrder(id));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyAuthority('inventory:inbound','user:manage')")
    public ApiResponse<PurchaseOrder> receive(@PathVariable Long id) {
        return ApiResponse.success(purchaseService.receiveOrder(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('purchase:create')")
    public ApiResponse<PurchaseOrder> cancel(@PathVariable Long id) {
        return ApiResponse.success(purchaseService.cancelOrder(id));
    }

    /**
     * 提交采购退货申请：创建一条 PENDING 状态的退货申请，
     * 等待仓管员审批（库存变动）后再由财务审批（金额变动）。
     */
    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyAuthority('purchase:create','purchase:approve')")
    public ApiResponse<ReturnRequestDTO> purchaseReturn(
            @PathVariable Long id, @Valid @RequestBody PurchaseReturnDTO dto) {
        return ApiResponse.success(returnRequestService.submitPurchaseReturn(id, dto));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('purchase:pay')")
    public ApiResponse<PurchaseOrder> recordPayment(
            @PathVariable Long id, @Valid @RequestBody OrderPaymentDTO dto) {
        return ApiResponse.success(purchaseService.recordPayment(id, dto.getAmount()));
    }
}
