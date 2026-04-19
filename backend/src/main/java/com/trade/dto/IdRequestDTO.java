package com.trade.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class IdRequestDTO {

    @NotNull(message = "ID不能为空")
    private Long id;
}
