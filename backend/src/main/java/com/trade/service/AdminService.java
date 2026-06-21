package com.trade.service;

import com.trade.dto.LoginRequest;
import com.trade.entity.Admin;
import com.trade.exception.BusinessException;
import com.trade.repository.AdminRepository;
import com.trade.security.JwtTokenProvider;
import com.trade.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    /** 平台默认租户上下文（仅用于 JWT，不承载业务数据） */
    private static final long PLATFORM_TENANT_ID = 1L;

    private final AdminRepository adminRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public Map<String, Object> platformLogin(LoginRequest loginRequest) {
        Admin admin = adminRepository.findByUsername(loginRequest.getAccount().trim())
                .orElseThrow(() -> new BusinessException("账号或密码错误"));
        if (admin.getStatus() != Admin.AdminStatus.ENABLED) {
            throw new BusinessException("账号已禁用");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(admin.getUsername(), loginRequest.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Map<String, Object> response = new HashMap<>();
        response.put("token", tokenProvider.generateAccessToken(authentication));
        response.put("refreshToken", tokenProvider.generateRefreshToken(
                admin.getUsername(),
                PLATFORM_TENANT_ID,
                Boolean.TRUE.equals(loginRequest.getRememberMe())));
        response.put("tokenType", "Bearer");
        response.put("tenantId", PLATFORM_TENANT_ID);
        response.put("tenantCode", "default");
        response.put("tenantName", "平台管理");
        response.put("userId", principal.getId());
        response.put("role", "PLATFORM_ADMIN");
        response.put("username", admin.getUsername());
        return response;
    }

    public Admin getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return adminRepository.findByUsername(principal.getUsername())
                    .orElseThrow(() -> new BusinessException("管理员不存在"));
        }
        throw new BusinessException("未登录");
    }

    public Admin requireCurrentAdmin() {
        Admin admin = getCurrentAdmin();
        if (admin.getStatus() != Admin.AdminStatus.ENABLED) {
            throw new BusinessException("账号已禁用");
        }
        return admin;
    }

    public Map<String, String> refreshAccessToken(String refreshToken, JwtTokenProvider tokenProvider) {
        var claims = tokenProvider.parseClaims(refreshToken);
        String username = claims.getSubject();
        adminRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("刷新令牌无效或已过期"));
        return tokenProvider.rotateTokens(refreshToken);
    }
}
