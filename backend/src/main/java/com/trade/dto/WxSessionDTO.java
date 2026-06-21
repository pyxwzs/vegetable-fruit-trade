package com.trade.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WxSessionDTO {

    private String openid;

    @JsonProperty("session_key")
    private String sessionKey;

    private String unionid;

    private Integer errcode;

    private String errmsg;
}
