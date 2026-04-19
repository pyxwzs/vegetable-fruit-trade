package com.trade.repository;

import com.trade.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {
    Optional<Customer> findByCustomerCode(String customerCode);
    Page<Customer> findByNameContaining(String name, Pageable pageable);
    boolean existsByCustomerCode(String customerCode);

    List<Customer> findByStatusOrderByIdAsc(Customer.CustomerStatus status);

    long countByStatus(Customer.CustomerStatus status);
}