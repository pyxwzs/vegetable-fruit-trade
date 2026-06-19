package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PaymentRecordDTO {
    private Long id;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private String paymentMethod;
    private LocalDateTime createdAt;
}
