package com.trade.repository;

import com.trade.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long>, JpaSpecificationExecutor<Supplier> {
    Optional<Supplier> findBySupplierCode(String supplierCode);
    Page<Supplier> findByNameContaining(String name, Pageable pageable);
    boolean existsBySupplierCode(String supplierCode);

    List<Supplier> findByStatusOrderByIdAsc(Supplier.SupplierStatus status);
}