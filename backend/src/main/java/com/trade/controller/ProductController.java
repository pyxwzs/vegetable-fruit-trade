package com.trade.controller;

import com.trade.dto.ProductDTO;
import com.trade.dto.ProductImportResultDTO;
import com.trade.entity.Product;
import com.trade.entity.ProductPriceHistory;
import com.trade.service.ProductService;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping("/lookup")
    @PreAuthorize("hasAuthority('product:view')")
    public ApiResponse<Product> lookupByCode(@RequestParam String code) {
        return ApiResponse.success(productService.lookupByCode(code));
    }

    @GetMapping(value = "/import/template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("hasAuthority('product:create')")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        byte[] bytes = productService.buildImportTemplateXlsx();
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("商品导入模板.xlsx", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('product:create')")
    public ApiResponse<ProductImportResultDTO> importProducts(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(productService.importFromExcel(file));
    }

    @GetMapping("/{id}/price-history")
    @PreAuthorize("hasAuthority('product:view')")
    public ApiResponse<List<ProductPriceHistory>> priceHistory(@PathVariable Long id) {
        return ApiResponse.success(productService.getPriceHistory(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('product:view')")
    public ApiResponse<Page<Product>> getProducts(
            @RequestParam(required = false) String keyword,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Product> products = productService.getProducts(keyword, pageable);
        return ApiResponse.success(products);
    }

    @GetMapping("/all-enabled")
    @PreAuthorize("hasAuthority('product:view')")
    public ApiResponse<List<Product>> getAllEnabledProducts() {
        List<Product> products = productService.getAllEnabledProducts();
        return ApiResponse.success(products);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:view')")
    public ApiResponse<Product> getProduct(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return ApiResponse.success(product);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('product:create')")
    public ApiResponse<Product> createProduct(@Valid @RequestBody ProductDTO productDTO) {
        Product product = productService.createProduct(productDTO);
        return ApiResponse.success(product);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:update')")
    public ApiResponse<Product> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductDTO productDTO) {
        Product product = productService.updateProduct(id, productDTO);
        return ApiResponse.success(product);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:delete')")
    public ApiResponse<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ApiResponse.success(null);
    }
}
