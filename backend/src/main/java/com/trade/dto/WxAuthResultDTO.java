package com.trade.dto;

import lombok.Data;

@Data
public class WxAuthResultDTO {

    /** 未注册时为 true，需跳转注册页 */
    private Boolean needRegister;

    private String token;
    private String refreshToken;
    private String tokenType;
    private Long tenantId;
    private String tenantCode;
    private String tenantName;
    private Long userId;
    private String role;

    /** 注册成功时返回 */
    private String username;
}
