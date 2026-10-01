package com.petshop.dto.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReportSummary(
        LocalDateTime generatedAt,
        ReportFilter filter,
        long totalOrders,
        long soldOrders,
        BigDecimal totalRevenue,
        BigDecimal averageTicket,
        long unitsSold,
        List<StatBreakdown> byStatus,
        List<StatBreakdown> byPaymentMethod,
        List<StatBreakdown> byCategory,
        List<ProductStat> topProducts,
        List<DailySale> dailySales,
        @JsonIgnore List<OrderRow> orders
) {
}