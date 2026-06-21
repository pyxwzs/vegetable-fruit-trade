package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class MenuKeysDTO {
    @NotEmpty(message = "请至少启用一项功能菜单")
    private List<String> menuKeys;
}
