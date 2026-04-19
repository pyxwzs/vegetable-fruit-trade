package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class StocktakeRequestDTO {
    @NotNull(message = "库存行不能为空")
    private Long inventoryId;

    @NotNull(message = "实盘数量不能为空")
    @DecimalMin(value = "0", inclusive = true, message = "实盘数量不能为负")
    private BigDecimal actualQuantity;

    private String remark;
}
