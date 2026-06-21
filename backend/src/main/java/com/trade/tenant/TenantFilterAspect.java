package com.trade.tenant;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 每个 Service 调用前启用 Hibernate 租户过滤器，避免跨租户看到演示/其他租户数据。
 */
@Aspect
@Component
@Order(0)
@RequiredArgsConstructor
public class TenantFilterAspect {

    private final TenantFilterManager tenantFilterManager;

    @Before("execution(* com.trade.service..*(..))")
    public void enableTenantFilter() {
        tenantFilterManager.enableIfPresent();
    }
}
