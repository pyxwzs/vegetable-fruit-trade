package com.trade.controller;

import com.trade.dto.TenantPublicDTO;
import com.trade.service.TenantService;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping("/public")
    public ApiResponse<TenantPublicDTO> getPublic(@RequestParam String code) {
        return ApiResponse.success(tenantService.getPublicInfo(code));
    }
}
