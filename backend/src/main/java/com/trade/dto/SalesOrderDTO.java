package com.trade.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Data
public class SalesOrderDTO {
    @NotNull(message = "客户不能为空")
    private Long customerId;

    private LocalDate orderDate;
    private LocalDate deliveryDate;
    private String paymentMethod;
    private String remark;

    @NotEmpty(message = "订单明细不能为空")
    @Valid
    private List<SalesOrderItemDTO> items;
}