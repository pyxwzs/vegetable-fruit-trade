package com.trade.service;

import com.trade.dto.MenuKeysDTO;
import com.trade.dto.UserDTO;
import com.trade.dto.WxAuthResultDTO;
import com.trade.dto.WxLoginRequest;
import com.trade.dto.WxSessionDTO;
import com.trade.entity.Tenant;
import com.trade.entity.User;
import com.trade.exception.BusinessException;
import com.trade.repository.AdminRepository;
import com.trade.repository.UserRepository;
import com.trade.security.CustomUserDetailsService;
import com.trade.security.JwtTokenProvider;
import com.trade.security.SecurityUtils;
import com.trade.security.UserPrincipal;
import com.trade.tenant.TenantContext;
import com.trade.tenant.TenantFilterManager;
import com.trade.util.MenuKeyUtils;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final JwtTokenProvider tokenProvider;
    private final TenantService tenantService;
    private final TenantFilterManager tenantFilterManager;
    private final WeChatMiniProgramService weChatMiniProgramService;
    private final CustomUserDetailsService customUserDetailsService;
    private final AdminService adminService;

    public WxAuthResultDTO wxLogin(WxLoginRequest request) {
        WxSessionDTO session = weChatMiniProgramService.code2Session(request.getWxCode());
        return userRepository.findByWxOpenId(session.getOpenid())
                .map(user -> {
                    if (user.getStatus() == User.UserStatus.DISABLED) {
                        throw new BusinessException("账号已禁用");
                    }
                    return buildWxAuthResult(user, false);
                })
                .orElseGet(() -> buildWxAuthResult(null, true));
    }

    public WxAuthResultDTO buildWxAuthResult(User user, boolean needRegister) {
        WxAuthResultDTO dto = new WxAuthResultDTO();
        dto.setNeedRegister(needRegister);
        if (needRegister || user == null) {
            return dto;
        }

        Tenant tenant = tenantService.requireActiveById(user.getTenantId());
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getLoginKey());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        dto.setToken(tokenProvider.generateAccessToken(authentication));
        dto.setRefreshToken(tokenProvider.generateRefreshToken(
                user.getLoginKey(), tenant.getId(), true));
        dto.setTokenType("Bearer");
        dto.setTenantId(tenant.getId());
        dto.setTenantCode(tenant.getCode());
        dto.setTenantName(tenant.getName());
        dto.setUserId(user.getId());
        dto.setRole("TENANT_ADMIN");
        dto.setUsername(user.getDisplayName());
        dto.setNeedRegister(false);
        return dto;
    }

    public Map<String, String> refreshAccessToken(String refreshToken) {
        try {
            if (!tokenProvider.validateToken(refreshToken) || !tokenProvider.validateRefreshToken(refreshToken)) {
                throw new BusinessException("刷新令牌无效或已过期");
            }
            var claims = tokenProvider.parseClaims(refreshToken);
            String subject = claims.getSubject();
            Long tenantId = tokenProvider.getTenantIdFromClaims(claims);
            if (tenantId == null) {
                throw new BusinessException("刷新令牌无效或已过期");
            }

            if (adminRepository.findByUsername(subject).isPresent()) {
                return adminService.refreshAccessToken(refreshToken, tokenProvider);
            }

            TenantContext.set(tenantId);
            tenantFilterManager.enableIfPresent();

            User user = userRepository.findByLoginKey(subject)
                    .orElseThrow(() -> new BusinessException("用户不存在"));
            if (!tenantId.equals(user.getTenantId())) {
                throw new BusinessException("刷新令牌无效或已过期");
            }
            if (user.getStatus() != User.UserStatus.ENABLED) {
                throw new BusinessException("账号已禁用");
            }
            return tokenProvider.rotateTokens(refreshToken);
        } catch (BusinessException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException("刷新令牌无效或已过期");
        }
    }

    public Object getCurrentAccount() {
        if (SecurityUtils.isPlatformAdmin()) {
            return adminService.getCurrentAdmin();
        }
        return getCurrentUser();
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            if (SecurityUtils.isPlatformAdmin()) {
                throw new BusinessException("当前为平台管理员会话");
            }
            return userRepository.findByLoginKey(principal.getUsername())
                    .orElseThrow(() -> new BusinessException("用户不存在"));
        }
        throw new BusinessException("未登录");
    }

    public Long resolveBizDataScopeUserId() {
        return TenantContext.getTenantId();
    }

    @Transactional
    public User updateUser(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (userDTO.getPhone() != null) {
            String phone = userDTO.getPhone().trim().isEmpty() ? null : userDTO.getPhone().trim();
            if (phone != null && !phone.equals(user.getPhone())
                    && userRepository.existsByPhoneAndIdNot(phone, id)) {
                throw new BusinessException("手机号已被使用");
            }
            user.setPhone(phone);
        }
        if (userDTO.getRealName() != null) {
            user.setRealName(userDTO.getRealName().trim().isEmpty() ? null : userDTO.getRealName().trim());
        }

        return userRepository.save(user);
    }

    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    @Transactional
    public User updateMenuKeys(MenuKeysDTO dto) {
        User user = getCurrentUser();
        List<String> normalized = MenuKeyUtils.normalizeAndValidate(dto.getMenuKeys());
        user.setMenuKeysJson(MenuKeyUtils.toJson(normalized));
        return userRepository.save(user);
    }
}
