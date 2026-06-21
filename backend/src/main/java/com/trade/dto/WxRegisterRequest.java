package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class WxRegisterRequest {

    @NotBlank(message = "请填写邀请码")
    private String inviteCode;

    @NotBlank(message = "请填写微信登录凭证")
    private String wxCode;

    @NotBlank(message = "请填写名称")
    @Size(max = 100, message = "名称最多100字")
    private String tenantName;

    @NotBlank(message = "请填写真实姓名")
    @Size(max = 50, message = "真实姓名最多50字")
    private String realName;

    @NotBlank(message = "请填写手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "请授权微信昵称")
    @Size(max = 100, message = "微信昵称最多100字")
    private String wxNickname;

    /** 头像 Base64（不含 data: 前缀），选填 */
    private String avatarBase64;
}
