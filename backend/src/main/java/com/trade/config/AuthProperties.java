package com.trade.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
/**
 * 认证相关开关，由管理员在 application.yml 中配置（修改后需重启服务）。
 */
@Data
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    /**
     * 是否启用登录邮箱二次验证。为 false 时用户通过 POST /auth/login 用户名密码一步登录；
     * 为 true 时需使用 /auth/login/mfa-challenge 与 /auth/login/mfa-verify。
     */
    private boolean mfaLoginEnabled = false;
}
