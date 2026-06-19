package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class UserDTO {
    private Long id;

    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;

    @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
    private String password;

    private String phone;
    private String realName;
}
