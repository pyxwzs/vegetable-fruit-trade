package com.trade.service;

import com.trade.dto.ExpenseDTO;
import com.trade.entity.Expense;
import com.trade.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public Page<Expense> list(String keyword, Integer year, Integer month,
                              LocalDate startDate, LocalDate endDate, int page, int size) {
        return expenseRepository.search(
                keyword != null && keyword.isBlank() ? null : keyword,
                year, month, startDate, endDate,
                PageRequest.of(page, size));
    }

    public java.math.BigDecimal sumByYear(int year) {
        return expenseRepository.sumByYear(year);
    }

    @Transactional
    public Expense create(ExpenseDTO dto) {
        Expense e = new Expense();
        e.setExpenseDate(dto.getExpenseDate());
        e.setCategory(dto.getCategory());
        e.setAmount(dto.getAmount());
        return expenseRepository.save(e);
    }

    @Transactional
    public Expense update(Long id, ExpenseDTO dto) {
        Expense e = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("支出记录不存在"));
        e.setExpenseDate(dto.getExpenseDate());
        e.setCategory(dto.getCategory());
        e.setAmount(dto.getAmount());
        return expenseRepository.save(e);
    }

    @Transactional
    public void delete(Long id) {
        expenseRepository.deleteById(id);
    }
}
