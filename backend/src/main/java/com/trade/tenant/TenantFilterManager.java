package com.trade.tenant;

import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Component
@RequiredArgsConstructor
public class TenantFilterManager {

    @PersistenceContext
    private EntityManager entityManager;

    public void enableIfPresent() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return;
        }
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
    }
}
