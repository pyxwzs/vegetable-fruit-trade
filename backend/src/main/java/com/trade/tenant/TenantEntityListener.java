package com.trade.tenant;

import com.trade.entity.TenantAwareEntity;

import javax.persistence.PrePersist;

public class TenantEntityListener {

    @PrePersist
    public void applyTenantId(Object entity) {
        if (!(entity instanceof TenantAwareEntity aware)) {
            return;
        }
        if (aware.getTenantId() == null) {
            aware.setTenantId(TenantContext.requireTenantId());
        }
    }
}
