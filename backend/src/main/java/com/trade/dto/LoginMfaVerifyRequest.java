package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class LoginMfaVerifyRequest {

    @NotBlank(message = "会话无效，请重新登录")
    private String sessionId;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码须为6位数字")
    private String code;
}
