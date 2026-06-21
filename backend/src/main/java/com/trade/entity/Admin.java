package com.trade.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/** 平台管理员（仅 Web /platform/login 账号密码登录） */
@Data
@Entity
@Table(name = "admins")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 50)
    private String realName;

    @Enumerated(EnumType.STRING)
    private AdminStatus status = AdminStatus.ENABLED;

    @CreationTimestamp
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;

    public enum AdminStatus {
        ENABLED, DISABLED
    }

    @com.fasterxml.jackson.annotation.JsonProperty("role")
    public String getRole() {
        return "PLATFORM_ADMIN";
    }
}
