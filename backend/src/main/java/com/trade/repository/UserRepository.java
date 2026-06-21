package com.trade.repository;

import com.trade.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByLoginKey(String loginKey);

    Optional<User> findByPhone(String phone);

    Optional<User> findByWxOpenId(String wxOpenId);

    boolean existsByLoginKey(String loginKey);

    boolean existsByPhone(String phone);

    boolean existsByWxOpenId(String wxOpenId);

    boolean existsByPhoneAndIdNot(String phone, Long id);
}
