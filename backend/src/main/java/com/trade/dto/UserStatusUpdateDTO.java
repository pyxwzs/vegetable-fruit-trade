package com.trade.dto;

import com.trade.entity.User;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class UserStatusUpdateDTO {
    @NotNull(message = "状态不能为空")
    private User.UserStatus status;
}
