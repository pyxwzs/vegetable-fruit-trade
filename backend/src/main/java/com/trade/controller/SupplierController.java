package com.trade.controller;

import com.trade.dto.SupplierDTO;
import com.trade.entity.Supplier;
import com.trade.repository.SupplierRepository;
import com.trade.service.SupplierService;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/suppliers")
@RequiredArgsConstructor
public class SupplierController {
    private final SupplierService supplierService;
    private final SupplierRepository supplierRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('purchase:view')")
    public ApiResponse<Page<Supplier>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(supplierService.getSuppliers(keyword, status, pageable));
    }

    /** 下拉选用：启用供应商（采购单创建时调用） */
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('purchase:view')")
    public ApiResponse<List<Supplier>> listActive() {
        return ApiResponse.success(supplierRepository.findByStatusOrderByIdAsc(Supplier.SupplierStatus.ACTIVE));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('purchase:view')")
    public ApiResponse<Supplier> get(@PathVariable Long id) {
        return ApiResponse.success(supplierService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('purchase:create')")
    public ApiResponse<Supplier> create(@Valid @RequestBody SupplierDTO dto) {
        return ApiResponse.success(supplierService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('purchase:create')")
    public ApiResponse<Supplier> update(@PathVariable Long id, @Valid @RequestBody SupplierDTO dto) {
        return ApiResponse.success(supplierService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('purchase:create')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ApiResponse.success(null);
    }
}
