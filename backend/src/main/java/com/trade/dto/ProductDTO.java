package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Data
public class ProductDTO {
    private Long id;

    @NotBlank(message = "商品编码不能为空")
    private String productCode;

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private Long categoryId;
    private String unit;
    private String specification;

    /** 条码；新建时留空则服务端自动生成唯一条码（一般无需填写） */
    private String barcode;

    private BigDecimal purchasePrice;

    private BigDecimal salePrice;

    private Integer shelfLife;
    private String imageUrl;
    private String description;
    private String status;
}