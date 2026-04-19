package com.trade.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Data
@Entity
@Table(name = "users")
@EqualsAndHashCode(exclude = "roles")
@ToString(exclude = "roles")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Column(unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 50)
    private String realName;

    /** 账号是否开启登录邮箱二次验证（还受系统 auth.mfa-login-enabled 约束） */
    @Column(name = "mfa_login_enabled", nullable = false)
    private Boolean mfaLoginEnabled = false;

    /** 是否在登录成功时向绑定邮箱发送提醒 */
    @Column(name = "login_alert_email_enabled", nullable = false)
    private Boolean loginAlertEmailEnabled = true;

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ENABLED;

    @ManyToMany(fetch = FetchType.EAGER)  // 改为立即加载
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Fetch(FetchMode.SUBSELECT)  // 使用子查询优化加载
    private Set<Role> roles = new HashSet<>();

    @CreationTimestamp
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;

    public enum UserStatus {
        ENABLED, DISABLED, LOCKED
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id != null && Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}