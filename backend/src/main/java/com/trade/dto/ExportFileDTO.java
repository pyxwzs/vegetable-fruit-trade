package com.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExportFileDTO {
    private byte[] content;
    private String filename;
}
