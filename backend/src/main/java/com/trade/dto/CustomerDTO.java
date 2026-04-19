package com.trade.dto;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CustomerDTO {

    private Long id;

    @NotBlank(message = "客户编码不能为空")
    @Size(max = 50, message = "客户编码长度不能超过50")
    private String customerCode;

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 100, message = "客户名称长度不能超过100")
    private String name;

    @Size(max = 50, message = "联系人长度不能超过50")
    private String contact;

    @Size(max = 20, message = "电话长度不能超过20")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 200, message = "地址长度不能超过200")
    private String address;

    @Size(max = 50, message = "税号长度不能超过50")
    private String taxNumber;

    private String type;

    private String creditLevel;

    @DecimalMin(value = "0.0", message = "信用额度必须大于等于0")
    private BigDecimal creditLimit;

    private BigDecimal totalPurchaseAmount;

    private Integer purchaseCount;

    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
