package com.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataScopeResponseDTO {
    /** 当前账号是否为管理员（可切换视角） */
    private boolean admin;
    /** 代查用户 id，未设置或非管理员时为 null */
    private Long targetUserId;
    private String targetUsername;
    private String targetRealName;
}
