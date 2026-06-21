package com.trade.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.trade.util.MenuKeyUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/** 租户用户（仅微信小程序授权登录） */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users")
public class User extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** JWT 内部标识，不对用户展示 */
    @Column(name = "login_key", unique = true, nullable = false, length = 64)
    @JsonIgnore
    private String loginKey;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 50)
    private String realName;

    @Column(name = "wx_openid", unique = true, nullable = false, length = 64)
    private String wxOpenId;

    @Column(name = "wx_nickname", length = 100)
    private String wxNickname;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ENABLED;

    @Column(name = "menu_keys", columnDefinition = "TEXT")
    @JsonIgnore
    private String menuKeysJson;

    @CreationTimestamp
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;

    @JsonProperty("menuKeys")
    public List<String> getMenuKeys() {
        return MenuKeyUtils.resolveEnabledKeys(menuKeysJson);
    }

    /** 展示用账号标识 */
    @JsonProperty("username")
    public String getDisplayName() {
        if (realName != null && !realName.isBlank()) {
            return realName;
        }
        if (wxNickname != null && !wxNickname.isBlank()) {
            return wxNickname;
        }
        return phone;
    }

    @JsonProperty("role")
    public String getRole() {
        return "TENANT_ADMIN";
    }

    public enum UserStatus {
        ENABLED, DISABLED
    }
}
