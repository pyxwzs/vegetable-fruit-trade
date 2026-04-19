package com.trade.dto;

import lombok.Data;

/**
 * 设置管理员数据视角；targetUserId 为 null 表示恢复为全量数据（删除代查记录）。
 */
@Data
public class DataScopeRequestDTO {
    private Long targetUserId;
}
