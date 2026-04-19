package com.trade.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "operation_logs")
public class OperationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String ip;

    private String module;

    private String action;

    @Column(columnDefinition = "TEXT")
    private String parameters; // 请求参数

    @Column(columnDefinition = "TEXT")
    private String result; // 返回结果

    private Long executionTime; // 执行时间(ms)

    private Boolean success;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private String userAgent;

    private String requestUrl;

    private String method;

    @CreationTimestamp
    private LocalDateTime createTime;
}