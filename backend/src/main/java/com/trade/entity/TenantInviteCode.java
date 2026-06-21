package com.trade.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tenant_invite_codes")
public class TenantInviteCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InviteStatus status = InviteStatus.UNUSED;

    @Column(name = "created_by_admin_id")
    private Long createdByAdminId;

    @Column(name = "used_by_tenant_id")
    private Long usedByTenantId;

    @Column(length = 200)
    private String remark;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(name = "create_time")
    private LocalDateTime createTime;

    public enum InviteStatus {
        UNUSED, USED
    }
}
