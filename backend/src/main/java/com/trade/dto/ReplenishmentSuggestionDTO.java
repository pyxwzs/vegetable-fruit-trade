package com.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReplenishmentSuggestionDTO {
    private Long productId;
    private String productName;
    private String unit;
    private BigDecimal currentAvailableTotal;
    /** 简单建议：将可用量补到约 30 的整数 */
    private BigDecimal suggestedQty;
}
