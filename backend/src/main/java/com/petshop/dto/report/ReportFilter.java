package com.petshop.dto.report;

import com.petshop.entity.OrderStatus;
import com.petshop.entity.PaymentMethod;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record ReportFilter(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        OrderStatus status,
        PaymentMethod paymentMethod,
        String category
) {
}