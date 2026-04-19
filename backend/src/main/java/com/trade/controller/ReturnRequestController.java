package com.trade.controller;

import com.trade.dto.ReturnRejectDTO;
import com.trade.dto.ReturnRequestDTO;
import com.trade.service.ReturnRequestService;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/return-requests")
@RequiredArgsConstructor
public class ReturnRequestController {

    private final ReturnRequestService returnRequestService;

    /**
     * 查询退货申请列表，可见范围由 Service 层按角色过滤：
     * - 仓管员：看到所有 PENDING / WH_APPROVED / WH_REJECTED
     * - 财务：看到所有 WH_APPROVED / FIN_APPROVED / FIN_REJECTED
     * - 采购员/销售员：只看自己提交的
     * - 管理员：全部
     */
    @GetMapping
    public ApiResponse<Page<ReturnRequestDTO>> list(
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(returnRequestService.getRequests(kind, status, pageable));
    }

    /** 仓管员审批通过 → 执行库存变动 */
    @PostMapping("/{id}/warehouse-approve")
    @PreAuthorize("hasAuthority('inventory:inbound') or hasAuthority('inventory:outbound')")
    public ApiResponse<ReturnRequestDTO> warehouseApprove(@PathVariable Long id) {
        return ApiResponse.success(returnRequestService.warehouseApprove(id));
    }

    /** 仓管员拒绝 */
    @PostMapping("/{id}/warehouse-reject")
    @PreAuthorize("hasAuthority('inventory:inbound') or hasAuthority('inventory:outbound')")
    public ApiResponse<ReturnRequestDTO> warehouseReject(
            @PathVariable Long id, @RequestBody(required = false) ReturnRejectDTO dto) {
        String reason = dto != null ? dto.getReason() : null;
        return ApiResponse.success(returnRequestService.warehouseReject(id, reason));
    }

    /** 财务审批通过 → 调整已付/已收金额 */
    @PostMapping("/{id}/finance-approve")
    @PreAuthorize("hasAnyAuthority('purchase:pay','sales:collect')")
    public ApiResponse<ReturnRequestDTO> financeApprove(@PathVariable Long id) {
        return ApiResponse.success(returnRequestService.financeApprove(id));
    }

    /** 财务拒绝 */
    @PostMapping("/{id}/finance-reject")
    @PreAuthorize("hasAnyAuthority('purchase:pay','sales:collect')")
    public ApiResponse<ReturnRequestDTO> financeReject(
            @PathVariable Long id, @RequestBody(required = false) ReturnRejectDTO dto) {
        String reason = dto != null ? dto.getReason() : null;
        return ApiResponse.success(returnRequestService.financeReject(id, reason));
    }
}
