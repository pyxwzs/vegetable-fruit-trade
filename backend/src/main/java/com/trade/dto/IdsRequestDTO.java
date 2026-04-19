package com.trade.dto;

import lombok.Data;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class IdsRequestDTO {

    @NotEmpty(message = "ID列表不能为空")
    private List<Long> ids;
}
