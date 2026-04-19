package com.trade.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class WarehouseDTO {

    private Long id;

    @NotBlank(message = "仓库编码不能为空")
    @Size(max = 50, message = "仓库编码长度不能超过50")
    private String code;

    @NotBlank(message = "仓库名称不能为空")
    @Size(max = 100, message = "仓库名称长度不能超过100")
    private String name;

    @Size(max = 200, message = "地址长度不能超过200")
    private String address;

    @Size(max = 50, message = "负责人长度不能超过50")
    private String manager;

    @Size(max = 20, message = "电话长度不能超过20")
    private String phone;

    private Double area;

    private String type;

    private String status;
}
