package com.trade.service;

import com.alibaba.fastjson.JSON;
import com.trade.dto.LoginMfaVerifyRequest;
import com.trade.dto.RegisterChallengeRequest;
import com.trade.entity.Role;
import com.trade.entity.User;
import com.trade.exception.BusinessException;
import com.trade.repository.RoleRepository;
import com.trade.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterService {

    private static final String REDIS_CODE = "trade:reg:code:";
    private static final String REDIS_SESSION = "trade:reg:session:";
    private static final int CODE_TTL_MINUTES = 15;
    private static final int SESSION_TTL_MINUTES = 20;
    private static final String DEFAULT_ROLE = "PURCHASER";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;
    private final NotificationService notificationService;

    /** 第一步：校验用户名/邮箱唯一性，发送邮箱验证码，不落库 */
    public Map<String, Object> challenge(RegisterChallengeRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("邮箱已被注册");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        PendingRegistration pending = new PendingRegistration();
        pending.setUsername(username);
        pending.setEmail(email);
        pending.setPasswordHash(passwordHash);
        pending.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        pending.setRealName(request.getRealName() != null ? request.getRealName().trim() : null);

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        try {
            stringRedisTemplate.opsForValue().set(REDIS_CODE + email, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("注册验证码写入 Redis 失败", e);
            throw new BusinessException("验证码服务暂不可用，请确认 Redis 已启动");
        }

        String sessionId = UUID.randomUUID().toString();
        String sessionJson = JSON.toJSONString(pending);
        try {
            stringRedisTemplate.opsForValue().set(REDIS_SESSION + sessionId, sessionJson, SESSION_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("注册会话写入 Redis 失败", e);
            throw new BusinessException("验证码服务暂不可用，请确认 Redis 已启动");
        }

        String body = "您正在注册果蔬批发商贸管理系统，验证码：" + code + "\n" + CODE_TTL_MINUTES + "分钟内有效。如非本人操作请忽略。";
        notificationService.sendEmail(email, "注册验证码", body);

        Map<String, Object> out = new HashMap<>();
        out.put("sessionId", sessionId);
        out.put("maskedEmail", maskEmail(email));
        return out;
    }

    /** 第二步：校验验证码与会话，创建用户并赋予默认角色 */
    @Transactional
    public User verify(LoginMfaVerifyRequest request) {
        String raw = stringRedisTemplate.opsForValue().get(REDIS_SESSION + request.getSessionId());
        if (raw == null || raw.isEmpty()) {
            throw new BusinessException("注册会话已过期，请重新填写注册信息");
        }

        PendingRegistration pending = JSON.parseObject(raw, PendingRegistration.class);
        String emailKey = pending.getEmail().toLowerCase();
        String cached = stringRedisTemplate.opsForValue().get(REDIS_CODE + emailKey);
        if (cached == null || !cached.equals(request.getCode().trim())) {
            throw new BusinessException("验证码错误或已过期");
        }

        if (userRepository.existsByUsername(pending.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        if (userRepository.existsByEmail(pending.getEmail())) {
            throw new BusinessException("邮箱已被注册");
        }

        Role role = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new BusinessException("系统未配置默认角色，请联系管理员"));

        User user = new User();
        user.setUsername(pending.getUsername());
        user.setPassword(pending.getPasswordHash());
        user.setEmail(pending.getEmail());
        user.setPhone(pending.getPhone());
        user.setRealName(pending.getRealName());
        user.setStatus(User.UserStatus.ENABLED);
        user.setRoles(new java.util.HashSet<>(Collections.singletonList(role)));

        User saved = userRepository.save(user);
        stringRedisTemplate.delete(REDIS_SESSION + request.getSessionId());
        stringRedisTemplate.delete(REDIS_CODE + emailKey);
        return saved;
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

    @Data
    private static class PendingRegistration {
        private String username;
        private String email;
        private String passwordHash;
        private String phone;
        private String realName;
    }
}
