package com.trade.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PurchaseReturnDTO {
    /** 退货出库仓库，默认主仓库 1 */
    private Long warehouseId;

    @NotEmpty(message = "退货明细不能为空")
    @Valid
    private List<PurchaseReturnLineDTO> lines;

    @Data
    public static class PurchaseReturnLineDTO {
        @NotNull(message = "商品不能为空")
        private Long productId;

        @NotNull(message = "退货数量不能为空")
        @Positive(message = "退货数量必须大于 0")
        private BigDecimal quantity;
    }
}
