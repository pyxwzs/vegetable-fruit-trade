package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PartnerProductLineDTO {
    private Long productId;
    private String productName;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal amount;
}
