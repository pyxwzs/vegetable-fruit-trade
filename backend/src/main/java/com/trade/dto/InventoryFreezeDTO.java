package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
public class InventoryFreezeDTO {
    @NotNull(message = "库存行不能为空")
    private Long inventoryId;

    @NotNull(message = "数量不能为空")
    @Positive(message = "数量必须大于 0")
    private BigDecimal quantity;
}
