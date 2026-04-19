package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class OrderPaymentDTO {
    @NotNull
    @DecimalMin(value = "0.01", message = "金额必须大于 0")
    private BigDecimal amount;
}
