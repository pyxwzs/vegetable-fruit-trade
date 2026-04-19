package com.trade.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseOrderDTO {
    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private String paymentMethod;
    private String remark;

    @NotEmpty(message = "订单明细不能为空")
    @Valid
    private List<PurchaseOrderItemDTO> items;
}