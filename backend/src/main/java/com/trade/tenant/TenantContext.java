package com.trade.tenant;

import com.trade.exception.BusinessException;

public final class TenantContext {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static Long requireTenantId() {
        Long tenantId = TENANT_ID.get();
        if (tenantId == null) {
            throw new BusinessException("缺少租户上下文");
        }
        return tenantId;
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}
