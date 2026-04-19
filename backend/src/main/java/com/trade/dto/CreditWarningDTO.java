package com.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditWarningDTO {
    private Long customerId;
    private String customerName;
    /** 授信额度 */
    private BigDecimal creditLimit;
    /** 当前未结清订单占用（约等于总额-已收） */
    private BigDecimal usedAmount;
    /** 是否已超过授信额度 */
    private boolean overLimit;
}
