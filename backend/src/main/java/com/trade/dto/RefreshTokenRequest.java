package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RefreshTokenRequest {
    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;
}
