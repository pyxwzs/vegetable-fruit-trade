package com.trade.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Data
public class LogQueryDTO extends PageRequestDTO {

    private String username;

    private String module;

    private String action;

    private Boolean success;

    private String ip;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Long minExecutionTime;

    private Long maxExecutionTime;
}