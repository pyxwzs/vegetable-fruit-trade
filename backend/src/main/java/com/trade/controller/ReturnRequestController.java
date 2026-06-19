package com.trade.controller;

import com.trade.dto.ReturnRequestDTO;
import com.trade.service.ReturnRequestService;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/return-requests")
@RequiredArgsConstructor
public class ReturnRequestController {

    private final ReturnRequestService returnRequestService;

    @GetMapping
    public ApiResponse<Page<ReturnRequestDTO>> list(
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(returnRequestService.getRequests(kind, status, pageable));
    }

    @PostMapping("/{id}/warehouse-approve")
    public ApiResponse<ReturnRequestDTO> warehouseApprove(@PathVariable Long id) {
        return ApiResponse.success(returnRequestService.warehouseApprove(id));
    }

    @PostMapping("/{id}/warehouse-reject")
    public ApiResponse<ReturnRequestDTO> warehouseReject(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ApiResponse.success(returnRequestService.warehouseReject(id, reason));
    }

    @PostMapping("/{id}/finance-approve")
    public ApiResponse<ReturnRequestDTO> financeApprove(@PathVariable Long id) {
        return ApiResponse.success(returnRequestService.financeApprove(id));
    }

    @PostMapping("/{id}/finance-reject")
    public ApiResponse<ReturnRequestDTO> financeReject(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ApiResponse.success(returnRequestService.financeReject(id, reason));
    }
}
