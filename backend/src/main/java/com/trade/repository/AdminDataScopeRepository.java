package com.trade.repository;

import com.trade.entity.AdminDataScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminDataScopeRepository extends JpaRepository<AdminDataScope, Long> {
}
