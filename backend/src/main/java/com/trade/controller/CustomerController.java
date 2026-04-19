package com.trade.controller;

import com.trade.dto.CreditWarningDTO;
import com.trade.dto.CustomerDTO;
import com.trade.entity.Customer;
import com.trade.repository.CustomerRepository;
import com.trade.service.CustomerService;
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
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerRepository customerRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('sales:view')")
    public ApiResponse<Page<Customer>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String creditLevel,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(customerService.getCustomers(keyword, creditLevel, pageable));
    }

    /** 下拉选用：启用客户（销售单创建时调用） */
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('sales:view')")
    public ApiResponse<List<Customer>> listActive() {
        return ApiResponse.success(customerRepository.findByStatusOrderByIdAsc(Customer.CustomerStatus.ACTIVE));
    }

    /** 授信占用达到额度 80% 及以上（含超额） */
    @GetMapping("/credit-warnings")
    @PreAuthorize("hasAuthority('sales:view')")
    public ApiResponse<List<CreditWarningDTO>> creditWarnings() {
        return ApiResponse.success(customerService.listCreditWarnings());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:view')")
    public ApiResponse<Customer> get(@PathVariable Long id) {
        return ApiResponse.success(customerService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sales:create')")
    public ApiResponse<Customer> create(@Valid @RequestBody CustomerDTO dto) {
        return ApiResponse.success(customerService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:create')")
    public ApiResponse<Customer> update(@PathVariable Long id, @Valid @RequestBody CustomerDTO dto) {
        return ApiResponse.success(customerService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:create')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ApiResponse.success(null);
    }
}
