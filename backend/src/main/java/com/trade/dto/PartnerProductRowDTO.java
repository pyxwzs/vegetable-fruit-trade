package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PartnerProductRowDTO {
    private Long productId;
    private String productName;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal totalAmount;
}
