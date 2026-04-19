package com.trade.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Set;

@Data
public class RoleDTO {

    private Long id;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称长度不能超过50")
    private String name;

    @Size(max = 200, message = "描述长度不能超过200")
    private String description;

    private Set<Long> permissionIds;

    private Set<PermissionDTO> permissions;
}
