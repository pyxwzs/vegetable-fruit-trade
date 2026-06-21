package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class SiteSettingsDTO {

    @NotBlank(message = "请填写系统名称")
    @Size(max = 100, message = "系统名称最多100字")
    private String siteName;

    private String logoPath;
}
