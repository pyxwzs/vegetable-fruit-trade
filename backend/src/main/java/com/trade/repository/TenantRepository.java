package com.trade.repository;

import com.trade.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByCode(String code);

    Optional<Tenant> findByCodeAndStatus(String code, Tenant.TenantStatus status);

    boolean existsByCode(String code);

    List<Tenant> findAllByOrderByIdAsc();
}
