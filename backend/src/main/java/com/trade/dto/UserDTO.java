package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;
import java.util.Set;

@Data
public class UserDTO {
    private Long id;

    /** 创建时必填，由服务层校验 */
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;

    @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
    private String password;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;
    private String realName;
    private String status;
    private Set<Long> roleIds;
}