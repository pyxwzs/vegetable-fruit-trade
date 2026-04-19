package com.trade.dto;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SupplierDTO {

    private Long id;

    @NotBlank(message = "供应商编码不能为空")
    @Size(max = 50, message = "供应商编码长度不能超过50")
    private String supplierCode;

    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 100, message = "供应商名称长度不能超过100")
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

    @Size(max = 100, message = "开户行长度不能超过100")
    private String bankName;

    @Size(max = 50, message = "银行账号长度不能超过50")
    private String bankAccount;

    @DecimalMin(value = "0.0", message = "信用评级必须大于等于0")
    @DecimalMax(value = "5.0", message = "信用评级不能超过5")
    private BigDecimal creditRating;

    @Min(value = 0, message = "准时交货率必须大于等于0")
    @Max(value = 100, message = "准时交货率不能超过100")
    private Integer deliveryOnTimeRate;

    @DecimalMin(value = "0.0", message = "质量合格率必须大于等于0")
    @DecimalMax(value = "100.0", message = "质量合格率不能超过100")
    private BigDecimal qualityPassRate;

    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}