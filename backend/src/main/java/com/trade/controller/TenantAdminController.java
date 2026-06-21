package com.trade.controller;

import com.trade.dto.*;
import com.trade.service.TenantAdminService;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class TenantAdminController {

    private final TenantAdminService tenantAdminService;

    @GetMapping("/tenants")
    public ApiResponse<List<TenantAdminDTO>> listTenants() {
        return ApiResponse.success(tenantAdminService.listTenants());
    }

    @PutMapping("/tenants/{id}/status")
    public ApiResponse<TenantAdminDTO> updateTenantStatus(@PathVariable Long id,
                                                          @Valid @RequestBody TenantStatusUpdateDTO dto) {
        return ApiResponse.success(tenantAdminService.updateTenantStatus(id, dto));
    }

    @GetMapping("/invite-codes")
    public ApiResponse<List<TenantInviteCodeDTO>> listInviteCodes() {
        return ApiResponse.success(tenantAdminService.listInviteCodes());
    }

    @PostMapping("/invite-codes")
    public ApiResponse<TenantInviteCodeDTO> generateInviteCode(@RequestBody(required = false) GenerateInviteCodeDTO dto) {
        return ApiResponse.success(tenantAdminService.generateInviteCode(dto));
    }
}
