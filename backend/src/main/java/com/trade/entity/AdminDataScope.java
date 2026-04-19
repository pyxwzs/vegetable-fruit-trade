package com.trade.entity;

import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 管理员当前「数据视角」代查对象，仅存数据库；JWT 只标识登录用户，数据范围每次请求按 admin_user_id 查表解析。
 */
@Data
@Entity
@Table(name = "admin_data_scope")
public class AdminDataScope {

    @Id
    @Column(name = "admin_user_id")
    private Long adminUserId;

    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @UpdateTimestamp
    private LocalDateTime updateTime;
}
