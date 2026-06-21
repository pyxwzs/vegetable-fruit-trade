package com.trade.controller;

import com.trade.dto.LogoUrlDTO;
import com.trade.dto.SiteSettingsDTO;
import com.trade.exception.BusinessException;
import com.trade.service.SiteSettingsService;
import com.trade.service.TenantService;
import com.trade.tenant.TenantContext;
import com.trade.tenant.TenantFilterManager;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;

@RestController
@RequestMapping("/site-settings")
@RequiredArgsConstructor
public class SiteSettingsController {

    private final SiteSettingsService siteSettingsService;
    private final TenantService tenantService;
    private final TenantFilterManager tenantFilterManager;

    @GetMapping
    public ApiResponse<SiteSettingsDTO> get(@RequestParam(required = false) String tenantCode) {
        if (TenantContext.getTenantId() == null && (tenantCode == null || tenantCode.isBlank())) {
            return ApiResponse.success(siteSettingsService.getPublicDefaultSettings());
        }
        ensureTenantContext(tenantCode);
        return ApiResponse.success(siteSettingsService.getSettings());
    }

    @PutMapping
    public ApiResponse<SiteSettingsDTO> update(@Valid @RequestBody SiteSettingsDTO dto) {
        tenantFilterManager.enableIfPresent();
        return ApiResponse.success(siteSettingsService.updateSiteName(dto));
    }

    @PostMapping("/logo")
    public ApiResponse<SiteSettingsDTO> uploadLogo(@RequestParam("file") MultipartFile file) throws IOException {
        tenantFilterManager.enableIfPresent();
        return ApiResponse.success(siteSettingsService.uploadLogo(file));
    }

    @PutMapping("/logo-url")
    public ApiResponse<SiteSettingsDTO> updateLogoUrl(@Valid @RequestBody LogoUrlDTO dto) {
        tenantFilterManager.enableIfPresent();
        return ApiResponse.success(siteSettingsService.updateLogoUrl(dto.getLogoUrl()));
    }

    private void ensureTenantContext(String tenantCode) {
        if (TenantContext.getTenantId() != null) {
            tenantFilterManager.enableIfPresent();
            return;
        }
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException("请指定租户编码");
        }
        tenantService.bindByCode(tenantCode);
        tenantFilterManager.enableIfPresent();
    }
}
