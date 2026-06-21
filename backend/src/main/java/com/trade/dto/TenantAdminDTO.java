package com.trade.dto;

import com.trade.entity.Tenant;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantAdminDTO {

    private Long id;
    private String code;
    private String name;
    private Tenant.TenantStatus status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
