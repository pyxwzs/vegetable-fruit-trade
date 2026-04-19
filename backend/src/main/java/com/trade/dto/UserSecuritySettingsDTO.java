package com.trade.dto;

import lombok.Data;

/**
 * 当前用户安全设置（二次登录、登录邮件提醒）
 */
@Data
public class UserSecuritySettingsDTO {

    /** 是否开启登录邮箱二次验证 */
    private Boolean mfaLoginEnabled;

    /** 是否接收登录成功提醒邮件 */
    private Boolean loginAlertEmailEnabled;
}
