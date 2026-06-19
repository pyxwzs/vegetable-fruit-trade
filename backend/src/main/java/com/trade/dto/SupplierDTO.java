package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
public class SupplierDTO {

    private Long id;

    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 100)
    private String name;

    @Size(max = 50)
    private String contact;

    @Size(max = 20)
    private String phone;

    @Size(max = 200)
    private String address;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
