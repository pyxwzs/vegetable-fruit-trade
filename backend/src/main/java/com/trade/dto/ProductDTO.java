package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ProductDTO {

    private Long id;

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String category;

    private String unit;

    private String specification;

    private String status;
}
