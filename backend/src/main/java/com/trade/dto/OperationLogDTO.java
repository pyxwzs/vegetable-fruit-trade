package com.trade.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OperationLogDTO {

    private Long id;

    private String username;

    private String ip;

    private String location;

    private String module;

    private String action;

    private String parameters;

    private String result;

    private Long executionTime;

    private Boolean success;

    private String errorMessage;

    private String userAgent;

    private String requestUrl;

    private String method;

    private LocalDateTime createTime;
}
