package com.trade.service;

import com.trade.dto.TenantPublicDTO;
import com.trade.entity.Tenant;
import com.trade.exception.BusinessException;
import com.trade.repository.TenantRepository;
import com.trade.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public Tenant requireActiveById(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException("租户不存在"));
        if (tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {
            throw new BusinessException("租户已停用，请联系管理员");
        }
        return tenant;
    }

    @Transactional(readOnly = true)
    public Tenant requireActiveByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("请填写租户编码");
        }
        Tenant tenant = tenantRepository.findByCodeAndStatus(code.trim(), Tenant.TenantStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException("租户不存在或已停用"));
        return tenant;
    }

    public void bindByCode(String code) {
        Tenant tenant = requireActiveByCode(code);
        TenantContext.set(tenant.getId());
    }

    @Transactional(readOnly = true)
    public TenantPublicDTO getPublicInfo(String code) {
        Tenant tenant = requireActiveByCode(code);
        TenantPublicDTO dto = new TenantPublicDTO();
        dto.setId(tenant.getId());
        dto.setCode(tenant.getCode());
        dto.setName(tenant.getName());
        return dto;
    }
}
