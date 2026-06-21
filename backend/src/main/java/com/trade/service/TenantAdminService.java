package com.trade.service;

import com.trade.dto.GenerateInviteCodeDTO;
import com.trade.dto.TenantAdminDTO;
import com.trade.dto.TenantInviteCodeDTO;
import com.trade.dto.TenantStatusUpdateDTO;
import com.trade.entity.Admin;
import com.trade.entity.Tenant;
import com.trade.entity.TenantInviteCode;
import com.trade.exception.BusinessException;
import com.trade.repository.AdminRepository;
import com.trade.repository.TenantInviteCodeRepository;
import com.trade.repository.TenantRepository;
import com.trade.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantAdminService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TenantRepository tenantRepository;
    private final TenantInviteCodeRepository inviteCodeRepository;
    private final AdminRepository adminRepository;
    private final AdminService adminService;

    @Transactional(readOnly = true)
    public List<TenantAdminDTO> listTenants() {
        requirePlatformAdmin();
        return tenantRepository.findAllByOrderByIdAsc().stream()
                .map(this::toTenantDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TenantAdminDTO updateTenantStatus(Long id, TenantStatusUpdateDTO dto) {
        requirePlatformAdmin();
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException("租户不存在"));
        if (tenant.getId() == 1L && dto.getStatus() == Tenant.TenantStatus.DISABLED) {
            throw new BusinessException("默认租户不可停用");
        }
        tenant.setStatus(dto.getStatus());
        return toTenantDto(tenantRepository.save(tenant));
    }

    @Transactional(readOnly = true)
    public List<TenantInviteCodeDTO> listInviteCodes() {
        requirePlatformAdmin();
        Map<Long, String> tenantNames = new HashMap<>();
        Map<Long, String> adminNames = new HashMap<>();
        tenantRepository.findAll().forEach(t -> tenantNames.put(t.getId(), t.getName()));
        adminRepository.findAll().forEach(a -> adminNames.put(a.getId(), a.getUsername()));

        return inviteCodeRepository.findAllByOrderByCreateTimeDesc().stream()
                .map(code -> toInviteDto(code, tenantNames, adminNames))
                .collect(Collectors.toList());
    }

    @Transactional
    public TenantInviteCodeDTO generateInviteCode(GenerateInviteCodeDTO dto) {
        Admin current = requirePlatformAdmin();
        String code = generateUniqueCode();
        TenantInviteCode invite = new TenantInviteCode();
        invite.setCode(code);
        invite.setStatus(TenantInviteCode.InviteStatus.UNUSED);
        invite.setCreatedByAdminId(current.getId());
        if (dto != null && dto.getRemark() != null && !dto.getRemark().isBlank()) {
            invite.setRemark(dto.getRemark().trim());
        }
        invite = inviteCodeRepository.save(invite);
        return toInviteDto(invite, Map.of(), Map.of(current.getId(), current.getUsername()));
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder sb = new StringBuilder(12);
            for (int i = 0; i < 12; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (!inviteCodeRepository.findByCode(code).isPresent()) {
                return code;
            }
        }
        throw new BusinessException("生成邀请码失败，请重试");
    }

    private Admin requirePlatformAdmin() {
        if (!SecurityUtils.isPlatformAdmin()) {
            throw new BusinessException("无权限执行此操作");
        }
        return adminService.requireCurrentAdmin();
    }

    private TenantAdminDTO toTenantDto(Tenant tenant) {
        TenantAdminDTO dto = new TenantAdminDTO();
        dto.setId(tenant.getId());
        dto.setCode(tenant.getCode());
        dto.setName(tenant.getName());
        dto.setStatus(tenant.getStatus());
        dto.setCreateTime(tenant.getCreateTime());
        dto.setUpdateTime(tenant.getUpdateTime());
        return dto;
    }

    private TenantInviteCodeDTO toInviteDto(TenantInviteCode code,
                                            Map<Long, String> tenantNames,
                                            Map<Long, String> adminNames) {
        TenantInviteCodeDTO dto = new TenantInviteCodeDTO();
        dto.setId(code.getId());
        dto.setCode(code.getCode());
        dto.setStatus(code.getStatus());
        dto.setCreatedByUserId(code.getCreatedByAdminId());
        dto.setCreatedByUsername(adminNames.get(code.getCreatedByAdminId()));
        dto.setUsedByTenantId(code.getUsedByTenantId());
        if (code.getUsedByTenantId() != null) {
            tenantRepository.findById(code.getUsedByTenantId())
                    .ifPresent(t -> dto.setUsedByTenantName(t.getName()));
        }
        dto.setRemark(code.getRemark());
        dto.setUsedAt(code.getUsedAt());
        dto.setCreateTime(code.getCreateTime());
        return dto;
    }
}
