package com.trade.dto;

import com.trade.entity.Tenant;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class TenantStatusUpdateDTO {

    @NotNull(message = "请指定状态")
    private Tenant.TenantStatus status;
}
