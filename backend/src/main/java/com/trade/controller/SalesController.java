package com.trade.controller;

import com.trade.dto.OrderPaymentDTO;
import com.trade.dto.ReturnRequestDTO;
import com.trade.dto.SalesOrderDTO;
import com.trade.dto.SalesReturnDTO;
import com.trade.entity.SalesOrder;
import com.trade.service.ReturnRequestService;
import com.trade.service.SalesService;
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
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SalesController {
    private final SalesService salesService;
    private final ReturnRequestService returnRequestService;

    @GetMapping
    @PreAuthorize("hasAuthority('sales:view')")
    public ApiResponse<Page<SalesOrder>> getSalesOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        SalesOrder.OrderStatus st = null;
        if (status != null && !status.isBlank()) {
            st = SalesOrder.OrderStatus.valueOf(status.trim().toUpperCase());
        }
        Page<SalesOrder> orders = salesService.getSalesOrders(keyword, st, pageable);
        return ApiResponse.success(orders);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:view')")
    public ApiResponse<SalesOrder> getSalesOrder(@PathVariable Long id) {
        SalesOrder order = salesService.getSalesOrder(id);
        return ApiResponse.success(order);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sales:create')")
    public ApiResponse<SalesOrder> createSalesOrder(@Valid @RequestBody SalesOrderDTO orderDTO) {
        SalesOrder order = salesService.createSalesOrder(orderDTO);
        return ApiResponse.success(order);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('sales:approve')")
    public ApiResponse<SalesOrder> approveOrder(@PathVariable Long id) {
        SalesOrder order = salesService.approveOrder(id);
        return ApiResponse.success(order);
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize("hasAnyAuthority('inventory:outbound','user:manage')")
    public ApiResponse<SalesOrder> shipOrder(
            @PathVariable Long id,
            @RequestParam(required = false) Long warehouseId) {
        SalesOrder order = salesService.shipOrder(id, warehouseId);
        return ApiResponse.success(order);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('sales:create')")
    public ApiResponse<SalesOrder> cancelOrder(@PathVariable Long id) {
        SalesOrder order = salesService.cancelOrder(id);
        return ApiResponse.success(order);
    }

    /**
     * 提交销售退货申请：创建一条 PENDING 状态的退货申请，
     * 等待仓管员审批（库存变动）→ 财务审批（金额变动）。
     */
    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyAuthority('sales:create','sales:approve')")
    public ApiResponse<ReturnRequestDTO> salesReturn(
            @PathVariable Long id, @Valid @RequestBody SalesReturnDTO dto) {
        return ApiResponse.success(returnRequestService.submitSalesReturn(id, dto));
    }

    /** 销售侧资金登记：已收金额与支付状态仅本接口变更 */
    @PostMapping("/{id}/collect")
    @PreAuthorize("hasAuthority('sales:collect')")
    public ApiResponse<SalesOrder> recordReceipt(
            @PathVariable Long id, @Valid @RequestBody OrderPaymentDTO dto) {
        return ApiResponse.success(salesService.recordReceipt(id, dto.getAmount()));
    }
}
