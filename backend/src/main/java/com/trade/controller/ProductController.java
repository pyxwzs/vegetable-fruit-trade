package com.trade.controller;

import com.trade.dto.ProductDTO;
import com.trade.entity.Product;
import com.trade.service.ProductService;
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
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<Page<Product>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 5, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(productService.getProducts(keyword, status, pageable));
    }

    @GetMapping("/all-enabled")
    public ApiResponse<List<Product>> getAllEnabledProducts() {
        return ApiResponse.success(productService.getAllEnabledProducts());
    }

    @GetMapping("/categories")
    public ApiResponse<List<String>> getCategories() {
        return ApiResponse.success(productService.getDistinctCategories());
    }

    @GetMapping("/{id}")
    public ApiResponse<Product> getProduct(@PathVariable Long id) {
        return ApiResponse.success(productService.getProductById(id));
    }

    @PostMapping
    public ApiResponse<Product> createProduct(@Valid @RequestBody ProductDTO productDTO) {
        return ApiResponse.success(productService.createProduct(productDTO));
    }

    @PutMapping("/{id}")
    public ApiResponse<Product> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductDTO productDTO) {
        return ApiResponse.success(productService.updateProduct(id, productDTO));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ApiResponse.success(null);
    }
}
