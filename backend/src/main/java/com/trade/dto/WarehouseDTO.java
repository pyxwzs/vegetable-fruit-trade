package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class WarehouseDTO {

    private Long id;

    @NotBlank(message = "仓库编码不能为空")
    @Size(max = 50)
    private String code;

    @NotBlank(message = "仓库名称不能为空")
    @Size(max = 100)
    private String name;

    private String status;
}
