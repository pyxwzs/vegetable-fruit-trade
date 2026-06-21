package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductTradeStatDTO {
    private Long productId;
    private String productName;
    private String unit;
    private BigDecimal purchaseQuantity = BigDecimal.ZERO;
    private BigDecimal purchaseAmount = BigDecimal.ZERO;
    private BigDecimal salesQuantity = BigDecimal.ZERO;
    private BigDecimal salesAmount = BigDecimal.ZERO;
}
