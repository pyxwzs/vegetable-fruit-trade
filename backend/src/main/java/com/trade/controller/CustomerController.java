package com.trade.controller;

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
    public ApiResponse<Page<Customer>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 5, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(customerService.getCustomers(keyword, status, pageable));
    }

    @GetMapping("/active")
    public ApiResponse<List<Customer>> listActive() {
        return ApiResponse.success(customerRepository.findByStatusOrderByIdAsc(Customer.CustomerStatus.ACTIVE));
    }

    @GetMapping("/{id}")
    public ApiResponse<Customer> get(@PathVariable Long id) {
        return ApiResponse.success(customerService.getById(id));
    }

    @PostMapping
    public ApiResponse<Customer> create(@Valid @RequestBody CustomerDTO dto) {
        return ApiResponse.success(customerService.create(dto));
    }

    @PutMapping("/{id}")
    public ApiResponse<Customer> update(@PathVariable Long id, @Valid @RequestBody CustomerDTO dto) {
        return ApiResponse.success(customerService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ApiResponse.success(null);
    }
}
