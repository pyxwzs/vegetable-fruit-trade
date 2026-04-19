package com.trade.repository;

import com.trade.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findByProductCode(String productCode);

    Optional<Product> findByBarcode(String barcode);

    @Query("SELECT p FROM Product p WHERE p.productCode = :code OR p.barcode = :code")
    Optional<Product> findByProductCodeOrBarcode(@Param("code") String code);

    Page<Product> findByNameContaining(String name, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId")
    List<Product> findByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT p FROM Product p WHERE p.status = 'ENABLED'")
    List<Product> findAllEnabled();

    boolean existsByProductCode(String productCode);

    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndIdNot(String barcode, Long id);
}