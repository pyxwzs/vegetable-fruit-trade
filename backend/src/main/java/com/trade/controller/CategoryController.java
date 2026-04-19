package com.trade.controller;

import com.trade.entity.Category;
import com.trade.repository.CategoryRepository;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    public ApiResponse<List<Category>> listCategories() {
        return ApiResponse.success(categoryRepository.findAll());
    }
}
