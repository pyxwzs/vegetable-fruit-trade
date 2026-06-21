package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class LogoUrlDTO {

    /** 为空则恢复默认 Logo */
    @Size(max = 500, message = "Logo 链接最多500字")
    private String logoUrl;
}
