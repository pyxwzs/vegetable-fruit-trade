package com.trade.service;

import com.trade.config.AuthProperties;
import com.trade.dto.LoginMfaVerifyRequest;
import com.trade.dto.LoginRequest;
import com.trade.entity.User;
import com.trade.exception.BusinessException;
import com.trade.repository.UserRepository;
import com.trade.security.JwtTokenProvider;
import com.trade.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MfaLoginService {

    private static final String REDIS_CODE = "trade:mfa:login:";
    private static final String REDIS_SESSION = "trade:mfa:session:";
    private static final int CODE_TTL_MINUTES = 5;
    private static final int SESSION_TTL_MINUTES = 10;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final NotificationService notificationService;
    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenProvider tokenProvider;
    private final AuthProperties authProperties;
    private final LoginAlertService loginAlertService;

    /**
     * 已校验密码且确定需 MFA 时：写 Redis、发验证码邮件，返回 sessionId。
     */
    public Map<String, Object> prepareChallenge(User user, LoginRequest loginRequest) {
        String email = user.getEmail();
        if (email == null || email.isBlank()) {
            throw new BusinessException("账户未绑定邮箱，无法发送登录验证码，请联系管理员维护邮箱");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String emailKey = email.trim().toLowerCase();
        try {
            stringRedisTemplate.opsForValue().set(REDIS_CODE + emailKey, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("登录验证码写入 Redis 失败", e);
            throw new BusinessException("验证码服务暂不可用，请确认 Redis 已启动");
        }

        String sessionId = UUID.randomUUID().toString();
        boolean rememberMe = Boolean.TRUE.equals(loginRequest.getRememberMe());
        String sessionPayload = user.getId() + "|" + rememberMe;
        try {
            stringRedisTemplate.opsForValue().set(REDIS_SESSION + sessionId, sessionPayload, SESSION_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("登录会话写入 Redis 失败", e);
            throw new BusinessException("验证码服务暂不可用，请确认 Redis 已启动");
        }

        String body = "您正在登录果蔬批发商贸管理系统，验证码：" + code + "\n" + CODE_TTL_MINUTES + "分钟内有效。如非本人操作请立即修改密码。";
        notificationService.sendEmail(email.trim(), "登录验证码", body);

        Map<String, Object> out = new HashMap<>();
        out.put("mfaRequired", true);
        out.put("sessionId", sessionId);
        out.put("maskedEmail", maskEmail(email.trim()));
        return out;
    }

    /**
     * 登录页「获取验证码」：校验密码后，若系统与账号均开启二次验证则发信。
     */
    public Map<String, Object> challenge(LoginRequest loginRequest) {
        if (!authProperties.isMfaLoginEnabled()) {
            throw new BusinessException("系统未开启登录二次验证功能");
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
        } finally {
            SecurityContextHolder.clearContext();
        }

        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (!Boolean.TRUE.equals(user.getMfaLoginEnabled())) {
            throw new BusinessException("当前账号未开启登录二次验证，请在个人资料中开启或使用一步登录");
        }

        return prepareChallenge(user, loginRequest);
    }

    /**
     * 第二步：校验邮箱验证码与会话，签发 access / refresh 令牌。
     */
    public Map<String, String> verify(LoginMfaVerifyRequest request) {
        if (!authProperties.isMfaLoginEnabled()) {
            throw new BusinessException("未开启登录二次验证");
        }
        String raw = stringRedisTemplate.opsForValue().get(REDIS_SESSION + request.getSessionId());
        if (raw == null || raw.isEmpty()) {
            throw new BusinessException("会话已过期，请重新输入账号密码");
        }
        String[] parts = raw.split("\\|", 2);
        if (parts.length < 2) {
            throw new BusinessException("会话数据无效");
        }
        long userId = Long.parseLong(parts[0]);
        boolean rememberMe = Boolean.parseBoolean(parts[1]);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (user.getStatus() != User.UserStatus.ENABLED) {
            throw new BusinessException("账号已禁用或锁定");
        }

        if (!Boolean.TRUE.equals(user.getMfaLoginEnabled())) {
            throw new BusinessException("当前账号未开启登录二次验证");
        }

        String emailKey = user.getEmail() != null ? user.getEmail().trim().toLowerCase() : "";
        if (emailKey.isEmpty()) {
            throw new BusinessException("账户邮箱无效");
        }

        String cached = stringRedisTemplate.opsForValue().get(REDIS_CODE + emailKey);
        if (cached == null || !cached.equals(request.getCode().trim())) {
            throw new BusinessException("验证码错误或已过期");
        }

        UserDetails details = userDetailsService.loadUserByUsername(user.getUsername());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                details, null, details.getAuthorities());

        String access = tokenProvider.generateAccessToken(authentication);
        String refresh = tokenProvider.generateRefreshToken(user.getUsername(), rememberMe);

        stringRedisTemplate.delete(REDIS_SESSION + request.getSessionId());
        stringRedisTemplate.delete(REDIS_CODE + emailKey);

        Map<String, String> response = new HashMap<>();
        response.put("token", access);
        response.put("refreshToken", refresh);
        response.put("type", "Bearer");
        response.put("tokenType", "Bearer");

        loginAlertService.sendIfEnabled(user);
        return response;
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        if (at == 1) {
            return "*" + email.substring(at);
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
