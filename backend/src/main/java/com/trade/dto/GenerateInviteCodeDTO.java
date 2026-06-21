package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class GenerateInviteCodeDTO {

    @Size(max = 200, message = "备注最多200字")
    private String remark;
}
