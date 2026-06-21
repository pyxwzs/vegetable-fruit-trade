package com.trade.repository;

import com.trade.entity.TenantInviteCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface TenantInviteCodeRepository extends JpaRepository<TenantInviteCode, Long> {

    Optional<TenantInviteCode> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM TenantInviteCode i WHERE i.code = :code AND i.status = com.trade.entity.TenantInviteCode$InviteStatus.UNUSED")
    Optional<TenantInviteCode> findUnusedForUpdate(@Param("code") String code);

    List<TenantInviteCode> findAllByOrderByCreateTimeDesc();
}
