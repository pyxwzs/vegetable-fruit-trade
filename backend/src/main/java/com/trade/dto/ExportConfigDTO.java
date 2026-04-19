package com.trade.dto;

import lombok.Data;
import java.util.List;

@Data
public class ExportConfigDTO {

    private String fileName;

    private String format; // excel, pdf, csv

    private List<String> columns;

    private List<ColumnConfig> columnConfigs;


    @Data
    public static class ColumnConfig {
        private String field;
        private String title;
        private Integer width;
        private String align; // left, center, right
        private String format; // date, currency, percentage
        private String formula;
    }
}
