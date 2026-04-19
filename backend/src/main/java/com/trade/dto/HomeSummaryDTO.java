package com.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeSummaryDTO {
    private DailyReportDTO dailyReport;
    private List<SalesTrendPointDTO> weekSalesTrend;
    private long stockKeepingProductCount;
    private long activeCustomerCount;
}
