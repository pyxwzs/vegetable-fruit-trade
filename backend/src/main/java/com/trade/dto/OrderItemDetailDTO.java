package com.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class OrderItemDetailDTO {
    private LocalDate date;
    private String partnerName;
    private String orderNo;
    private String paymentStatus;
    private String productName;
    private String unit;
    private String specification;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
}
