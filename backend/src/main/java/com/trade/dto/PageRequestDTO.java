package com.trade.dto;

import lombok.Data;
import javax.validation.constraints.Min;

@Data
public class PageRequestDTO {
    @Min(value = 1, message = "页码最小为1")
    private Integer page = 1;

    @Min(value = 1, message = "每页大小最小为1")
    private Integer size = 10;

    private String keyword;

    private String sortField;

    private String sortOrder = "desc";
}
