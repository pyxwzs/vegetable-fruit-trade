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

    @PostMapping("/{id}/approve")
    public ApiResponse<ReturnRequestDTO> approve(
            @PathVariable Long id,
            @RequestParam(required = false) Long warehouseId) {
        return ApiResponse.success(returnRequestService.approve(id, warehouseId));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<ReturnRequestDTO> reject(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ApiResponse.success(returnRequestService.reject(id, reason));
    }
}
