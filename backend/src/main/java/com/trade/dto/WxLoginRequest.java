package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class WxLoginRequest {

    @NotBlank(message = "微信登录凭证无效")
    private String wxCode;
}
