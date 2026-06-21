package com.trade.dto;

import com.trade.entity.TenantInviteCode;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantInviteCodeDTO {

    private Long id;
    private String code;
    private TenantInviteCode.InviteStatus status;
    private Long createdByUserId;
    private String createdByUsername;
    private Long usedByTenantId;
    private String usedByTenantName;
    private String remark;
    private LocalDateTime usedAt;
    private LocalDateTime createTime;
}
