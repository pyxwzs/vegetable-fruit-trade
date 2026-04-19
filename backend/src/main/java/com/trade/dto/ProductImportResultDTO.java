package com.trade.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProductImportResultDTO {
    private int successCount;
    private int failCount;
    private List<String> errors = new ArrayList<>();
}
