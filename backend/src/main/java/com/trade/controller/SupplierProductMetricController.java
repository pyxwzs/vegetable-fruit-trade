package com.trade.controller;

import com.trade.dto.SupplierMetricCompletionReportDTO;
import com.trade.dto.SupplierProductMetricDTO;
import com.trade.service.SupplierProductMetricService;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/supplier-product-metrics")
@RequiredArgsConstructor
public class SupplierProductMetricController {

    private final SupplierProductMetricService metricService;

    @GetMapping
    public ApiResponse<Page<SupplierProductMetricDTO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String periodType,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @PageableDefault(size = 5, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(
                metricService.page(keyword, supplierId, productId, status, periodType, year, month, pageable));
    }

    @GetMapping("/completion")
    public ApiResponse<SupplierMetricCompletionReportDTO> completion(
            @RequestParam(required = false) String periodType,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long supplierId) {
        return ApiResponse.success(metricService.completionReport(periodType, year, month, supplierId));
    }

    @GetMapping("/{id}")
    public ApiResponse<SupplierProductMetricDTO> get(@PathVariable Long id) {
        return ApiResponse.success(metricService.getById(id));
    }

    @PostMapping
    public ApiResponse<SupplierProductMetricDTO> create(@Valid @RequestBody SupplierProductMetricDTO dto) {
        return ApiResponse.success(metricService.create(dto));
    }

    @PutMapping("/{id}")
    public ApiResponse<SupplierProductMetricDTO> update(@PathVariable Long id,
                                                        @Valid @RequestBody SupplierProductMetricDTO dto) {
        return ApiResponse.success(metricService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        metricService.delete(id);
        return ApiResponse.success(null);
    }
}
