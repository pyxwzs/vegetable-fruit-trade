package com.trade.controller;

import com.trade.dto.ExpenseDTO;
import com.trade.entity.Expense;
import com.trade.service.ExpenseService;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public ApiResponse<Page<Expense>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ApiResponse.success(expenseService.list(keyword, year, month, startDate, endDate, page, size));
    }

    @GetMapping("/year-total")
    public ApiResponse<java.math.BigDecimal> yearTotal(
            @RequestParam(defaultValue = "0") int year) {
        int y = year > 0 ? year : LocalDate.now().getYear();
        return ApiResponse.success(expenseService.sumByYear(y));
    }

    @GetMapping("/categories")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.success(expenseService.getDistinctCategories());
    }

    @PostMapping
    public ApiResponse<Expense> create(@RequestBody ExpenseDTO dto) {
        return ApiResponse.success(expenseService.create(dto));
    }

    @PutMapping("/{id}")
    public ApiResponse<Expense> update(@PathVariable Long id, @RequestBody ExpenseDTO dto) {
        return ApiResponse.success(expenseService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        expenseService.delete(id);
        return ApiResponse.success();
    }
}
