package com.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 退货申请单行明细（序列化存入 linesJson 字段，同时作为列表展示 DTO） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestLineDTO {
    private Long productId;
    private String productName;
    private BigDecimal quantity;
    private BigDecimal price;
}
