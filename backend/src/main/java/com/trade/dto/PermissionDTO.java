package com.trade.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class PermissionDTO {

    private Long id;

    @NotBlank(message = "权限编码不能为空")
    @Size(max = 100, message = "权限编码长度不能超过100")
    private String code;

    @Size(max = 100, message = "权限名称长度不能超过100")
    private String name;

    @Size(max = 200, message = "描述长度不能超过200")
    private String description;

    @Size(max = 50, message = "模块名称长度不能超过50")
    private String module;

    @Size(max = 50, message = "操作名称长度不能超过50")
    private String action;
}
