package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 勾选「记住我」时刷新令牌有效期更长（30天 vs 7天） */
    private Boolean rememberMe;
}
