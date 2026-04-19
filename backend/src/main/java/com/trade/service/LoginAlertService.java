package com.trade.service;

import com.trade.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 登录成功后按用户偏好发送「登录提醒」邮件（非验证码邮件）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAlertService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NotificationService notificationService;

    public void sendIfEnabled(User user) {
        if (!Boolean.TRUE.equals(user.getLoginAlertEmailEnabled())) {
            return;
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        String ip = resolveClientIp();
        String body = String.format(
                "您的账号「%s」于 %s 登录果蔬批发商贸管理系统。\n如非本人操作，请立即修改密码。\n登录 IP：%s",
                user.getUsername(),
                LocalDateTime.now().format(TS),
                ip != null ? ip : "未知"
        );
        notificationService.sendEmail(user.getEmail().trim(), "登录提醒", body);
    }

    private static String resolveClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest req = attrs.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
            return req.getRemoteAddr();
        } catch (Exception e) {
            log.debug("解析客户端 IP 失败: {}", e.getMessage());
            return null;
        }
    }
}
