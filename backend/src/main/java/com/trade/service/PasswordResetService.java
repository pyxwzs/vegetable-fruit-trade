package com.trade.service;

import com.trade.entity.User;
import com.trade.exception.BusinessException;
import com.trade.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String REDIS_PREFIX = "trade:pwdreset:";
    private static final int CODE_TTL_MINUTES = 15;

    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 发送重置验证码到邮箱（若邮箱未注册也返回成功提示，避免枚举用户）
     */
    public void sendResetCode(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (!userOpt.isPresent()) {
            log.warn("密码重置请求：邮箱未注册 {}", email);
            return;
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String key = REDIS_PREFIX + email.toLowerCase();
        try {
            stringRedisTemplate.opsForValue().set(key, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("写入验证码缓存失败", e);
            throw new BusinessException("验证码服务暂不可用，请确认 Redis 已启动");
        }

        String body = "您正在重置果蔬批发商贸管理系统账户密码，验证码为：" + code + "\n" + CODE_TTL_MINUTES + "分钟内有效。如非本人操作请忽略。";
        notificationService.sendEmail(email, "密码重置验证码", body);
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        String key = REDIS_PREFIX + email.toLowerCase();
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached == null || !cached.equals(code.trim())) {
            throw new BusinessException("验证码无效或已过期");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        stringRedisTemplate.delete(key);
    }
}
