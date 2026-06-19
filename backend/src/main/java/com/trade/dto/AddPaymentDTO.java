package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AddPaymentDTO {
    private LocalDate paymentDate;
    private BigDecimal amount;
    private String paymentMethod;
}
