package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
public class TransferRequestDTO {
    @NotNull(message = "源库存行不能为空")
    private Long sourceInventoryId;

    @NotNull(message = "目标仓库不能为空")
    private Long toWarehouseId;

    @NotNull(message = "调拨数量不能为空")
    @Positive(message = "调拨数量必须大于 0")
    private BigDecimal quantity;

    private String remark;
}
