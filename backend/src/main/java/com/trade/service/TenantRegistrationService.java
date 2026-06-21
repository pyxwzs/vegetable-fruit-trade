package com.trade.service;

import com.trade.dto.WxRegisterRequest;
import com.trade.dto.WxSessionDTO;
import com.trade.entity.*;
import com.trade.exception.BusinessException;
import com.trade.repository.*;
import com.trade.tenant.TenantContext;
import com.trade.tenant.TenantFilterManager;
import com.trade.util.TenantCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantRegistrationService {

    private final TenantInviteCodeRepository inviteCodeRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final SiteSettingsRepository siteSettingsRepository;
    private final TenantFilterManager tenantFilterManager;
    private final TenantCodeGenerator tenantCodeGenerator;
    private final WeChatMiniProgramService weChatMiniProgramService;
    private final AvatarStorageService avatarStorageService;

    @Transactional
    public User registerWx(WxRegisterRequest request) {
        WxSessionDTO session = weChatMiniProgramService.code2Session(request.getWxCode());
        String openId = session.getOpenid();
        if (userRepository.existsByWxOpenId(openId)) {
            throw new BusinessException("该微信已注册，请直接登录");
        }

        String phone = request.getPhone().trim();
        if (userRepository.existsByPhone(phone)) {
            throw new BusinessException("手机号已被使用");
        }

        String inviteCode = request.getInviteCode().trim().toUpperCase();
        TenantInviteCode invite = inviteCodeRepository.findUnusedForUpdate(inviteCode)
                .orElseThrow(() -> new BusinessException("邀请码无效或已被使用"));

        String tenantCode = tenantCodeGenerator.generateUniqueCode();
        String tenantName = request.getTenantName().trim();
        Tenant tenant = new Tenant();
        tenant.setCode(tenantCode);
        tenant.setName(tenantName);
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant = tenantRepository.save(tenant);

        TenantContext.set(tenant.getId());
        tenantFilterManager.enableIfPresent();

        Warehouse warehouse = new Warehouse();
        warehouse.setTenantId(tenant.getId());
        warehouse.setCode("WH001");
        warehouse.setName("主仓库");
        warehouseRepository.save(warehouse);

        SiteSettings siteSettings = new SiteSettings();
        siteSettings.setTenantId(tenant.getId());
        siteSettings.setSiteName(tenantName);
        siteSettingsRepository.save(siteSettings);

        User user = new User();
        user.setTenantId(tenant.getId());
        user.setLoginKey(buildLoginKey(openId));
        user.setPhone(phone);
        user.setRealName(request.getRealName().trim());
        user.setWxOpenId(openId);
        user.setWxNickname(request.getWxNickname().trim());
        user.setStatus(User.UserStatus.ENABLED);
        user = userRepository.save(user);

        String avatarPath = avatarStorageService.saveUserAvatar(
                tenant.getId(), user.getId(), request.getAvatarBase64());
        if (avatarPath != null) {
            user.setAvatarUrl(avatarPath);
            userRepository.save(user);
        }

        invite.setStatus(TenantInviteCode.InviteStatus.USED);
        invite.setUsedByTenantId(tenant.getId());
        invite.setUsedAt(java.time.LocalDateTime.now());
        inviteCodeRepository.save(invite);

        return user;
    }

    private String buildLoginKey(String openId) {
        String suffix = openId.replaceAll("[^a-zA-Z0-9]", "");
        if (suffix.length() > 24) {
            suffix = suffix.substring(suffix.length() - 24);
        }
        String key = "u_" + suffix;
        if (!userRepository.existsByLoginKey(key)) {
            return key;
        }
        return "u_" + suffix + "_" + System.currentTimeMillis() % 10000;
    }
}
