package com.trade.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class StatusUpdateDTO {

    @NotNull(message = "ID不能为空")
    private Long id;

    @NotNull(message = "状态不能为空")
    private String status;

    private String reason;
}
